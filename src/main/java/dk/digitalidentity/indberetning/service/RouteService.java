package dk.digitalidentity.indberetning.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.exceptions.AddressLookupRuntimeException;
import dk.digitalidentity.indberetning.model.dao.RouteDao;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    @Autowired
    @Qualifier("defaultRestClient")
    private RestClient restClient;

    private final OS2indberetningConfiguration configuration;
    private final RouteDao routeDao;

    @Getter
    @Setter
    private Map<String, Integer> statisticsMap = new ConcurrentHashMap<>();

    public Route save(Route route) { return routeDao.save(route); }

    //Methods for encoding and decoding Polylines
    public String convertGoogleLatLngToGeoJsonRoute(List<LatLng> googleLatLng){
        if (googleLatLng.isEmpty()) { return "{\"coordinates\": []}"; }
        String coords = "{\"geometry\": {\"type\": \"LineString\", \"coordinates\": [";
        for (LatLng latLng : googleLatLng){
            coords += "["+latLng.lng+","+latLng.lat+"],";
        }
        coords = coords.substring(0,coords.length()-1);
        coords += "]}}";
        return coords;
    }

    public String routeEncoder(String unparsedRouteString) {
        String[] coordinates = unparsedRouteString.split(",");
        List<LatLng> latLngList = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            latLngList.add(new LatLng(Double.parseDouble(coordinates[i+1]), Double.parseDouble(coordinates[i])));
        }

        EncodedPolyline encodedPoly = new EncodedPolyline(latLngList);
        String stringEncodedPolyline = encodedPoly.toString();
        return stringEncodedPolyline.substring(18, stringEncodedPolyline.length()-1); //removes  the front '[EncodedPolyline: ' and end ']'
    }

    public String encodeGpsToPolyline(List<GpsCoordinate> gpsList) {
        List<LatLng> latLngList = new ArrayList<>();

        gpsList.forEach(gps -> latLngList.add(new LatLng(gps.getLatitude(), gps.getLongitude())));

        return encodeLatLngToPolyline(latLngList);
    }
    public String encodeLatLngToPolyline(List<LatLng> latLngList) {
        EncodedPolyline encodedPoly = new EncodedPolyline(latLngList);
        String stringEncodedPolyline = encodedPoly.toString();
        return stringEncodedPolyline.substring(18, stringEncodedPolyline.length() - 1); //removes  the front '[EncodedPolyline: ' and end ']'
    }

    public List<LatLng> decodePolyline(EncodedPolyline encodedPolyline) {
        return encodedPolyline.decodePath();
    }

    public record Result(double distance, Geometry geometry, List<List<Double>> coordinates, Double estimatedTravelTime, List<Double> legDurations, String error) {}
    public record Geometry(List<List<Double>> coordinates, String type) {}
    @Nullable
    public Result getRoute(String coordinates, String functionName) throws JsonProcessingException {
        if (functionName == null) {
            // This means "available-information" is calling some distance function, we hardcode the name here simply
            functionName = "reportService.availableInformation";
        }

		if (log.isDebugEnabled()) {
            log.debug("coordinates: " + coordinates);
		}

        boolean existing = statisticsMap.containsKey(functionName);

        if (existing) {
            statisticsMap.replace(functionName, statisticsMap.get(functionName) + 1);
        }
        else {
            statisticsMap.put(functionName, 1);
        }
		try {
			String coordinatesString = coordinates.replace("[", "").replace("],", ";").replace("]", "");
			String response = restClient.get()
					.uri(configuration.getMap().getSeptimaBaseUrl() + "route/v1/car/" + coordinatesString + "?access_token=" + configuration.getMap().getSeptimaApikey() + "&overview=full&alternatives=false&geometries=geojson")
                    .retrieve()
					.body(String.class);

			return readResponse(response);
		}
		catch (HttpClientErrorException | HttpServerErrorException e) {
			log.warn("Exception occured when trying to create route:" + e.getResponseBodyAsString());
			if (e.getStatusCode() != HttpStatus.BAD_GATEWAY && e.getStatusCode() != HttpStatus.GATEWAY_TIMEOUT && e.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR) {
				// Intentionally isolating this, as there may be more codes associated with 400 gateway
				// Simply add them to the front-end when they appear
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
                JsonNode codeNode = root.get("code");
                // We assume a timeout of some sort
                String code = (codeNode != null && !codeNode.isNull()) ? codeNode.asText() : "500";
				return new Result(0, null, null, null, null, code);
			}
			return new Result(0, null, null, null, null, e.getStatusCode().toString());
		}
        catch (ResourceAccessException  e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.warn("Socket Timeout occured when trying to fetch route from septima");
            }
            return new Result(0, null, null, null, null, "500");
        }
    }

    private Result readResponse(String response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("response: {}", response);
        JsonNode rootNode = objectMapper.readTree(response);
        if (rootNode.has("routes")) {
            JsonNode routeNode = rootNode.get("routes").get(0);
            JsonNode distanceNode = routeNode.get("distance");
            JsonNode durationNode = routeNode.get("duration");
            JsonNode geometryNode = routeNode.get("geometry");
            JsonNode legsNode = routeNode.get("legs");

            if (distanceNode != null && geometryNode != null && durationNode != null && geometryNode.has("coordinates")) {
                List<Double> legDurations = new ArrayList<>();
                if (legsNode != null && legsNode.isArray()) {
                    for (JsonNode leg : legsNode) {
                        JsonNode legDuration = leg.get("duration");
                        if (legDuration != null) {
                            legDurations.add(legDuration.asDouble());
                        }
                    }
                }
                return convertResponseToResult(distanceNode.asDouble(), durationNode.asDouble(), geometryNode.get("coordinates"), legDurations);
            }

            log.warn("Route response missing required fields: {}", response);
            return null;
        }
        log.warn("Route response missing 'routes' key: {}", response);
        return null;
    }

	private Result convertResponseToResult(Double distance, Double duration, JsonNode coordinatesNode, List<Double> legDurations) {
        List<LatLng> coordinates = new ArrayList<>();
        for (JsonNode jsonNode : coordinatesNode) {
            LatLng latLng = new LatLng(jsonNode.get(0).asDouble(), jsonNode.get(1).asDouble());
            coordinates.add(latLng);
        }
		List<List<Double>> list = coordinates.stream().map(latLng -> List.of(latLng.lat, latLng.lng)).toList();
        return new Result(distance, new Geometry(list, "LineString"), list, duration, legDurations, null);

    }

    public double distanceBetweenAddresses(Address source, Address destination) throws JsonProcessingException {
        if (log.isTraceEnabled()) {
            log.trace("Source address: " + source.getAddressString());
            log.trace("Destination address: " + destination.getAddressString());
            log.trace("source: " + source.getLongitude() + ", " + source.getLatitude());
            log.trace("destination: " + destination.getLongitude() + ", " + destination.getLatitude());
        }

        if (source.getLatitude() == 0.0 || source.getLongitude() == 0.0 || destination.getLatitude() == 0.0 || destination.getLongitude() == 0.0) {
            log.warn("The coordinates for {} or {} are 0", source.getAddressString(), destination.getAddressString());
            return 0.0;
        }
        Result routeString = getRoute("[" + source.getLongitude() + "," + source.getLatitude() + "],[" + destination.getLongitude() + "," + destination.getLatitude() + "]", "distanceBetweenAddresses");
        return routeString.distance / 1000;
    }

    @Nullable
    public String latLngToAddress(double lat, double lng) throws AddressLookupRuntimeException {
        return restClient.get()
                .uri(configuration.getMap().getMapServiceBaseUrl() + "/adgangsadresser/reverse?x=" + lng + "&y=" + lat)
                .header("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .header("Content-Type", "application/json; charset=utf-8")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig", response.getStatusCode(), response.getHeaders());
                })
                .body(String.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressDTO(String number, String road, int postcode, String city) { }
    @Nullable
    public AddressDTO latLngToAddressObj(double lat, double lng) {
        Assert.notNull(lat, "Lat must be non null");
        Assert.notNull(lng, "Lng must be non null");

        String unparsedAddressResponse = latLngToAddress(lat, lng);

        ObjectMapper mapper = new ObjectMapper();
		JsonNode addressJson = null;
		try {
			addressJson = mapper.readTree(unparsedAddressResponse);
		}
		catch (JsonProcessingException ex) {
            log.warn("Could not process json structure", ex);
			return null;
		}

		if (addressJson == null || addressJson.isEmpty()) {
            log.warn("No addresses found from supplied coordinates");
            return null;
        }

        JsonNode addressNode = addressJson.get(0);
        if (addressNode == null) {
            addressNode = addressJson;
        }

        JsonNode roadNode = addressNode.get("vejstykke");
        if (roadNode == null || roadNode.get("navn") == null || !StringUtils.hasLength(roadNode.get("navn").toString())) {
            log.warn("'vejstykke' was empty");
            return null;
        }
        String road = roadNode.get("navn").toString(); //vejnavn

        JsonNode houseNumberNode = addressNode.get("husnr");
        if (houseNumberNode == null || !StringUtils.hasLength(houseNumberNode.toString())) {
            log.warn("'husnr' was empty");
            return null;
        }
        String houseNumber = houseNumberNode.toString(); //husnummer

        JsonNode postCodeNode = addressNode.get("postnummer");
        if (postCodeNode == null) {
            log.warn("'postnummer' was empty");
            return null;
        }

        if (postCodeNode.get("nr") == null || postCodeNode.asInt(-1) != -1) {
            log.warn("'postnummer nr' was empty");
            return null;
        }
        int postCode = postCodeNode.get("nr").asInt(-1); //postnummer

        if (postCodeNode.get("navn") == null || !StringUtils.hasLength(postCodeNode.get("navn").toString())) {
            log.warn("'postnummer navn' was empty");
            return null;
        }
        String postCodeName = postCodeNode.get("navn").toString(); //postnavn

        return new AddressDTO(houseNumber, road, postCode, postCodeName);
    }

	public boolean existsById(Long id) {
		return routeDao.existsById(id);
	}

	public List<Route> findAllById(List<Long> ids) {
		return routeDao.findAllById(ids);
	}

	public String formatDuration(double durationSeconds) {
		long totalMinutes = Math.round(durationSeconds / 60);
		long hours = totalMinutes / 60;
		long minutes = totalMinutes % 60;

		if (hours == 0) {
			return minutes + " minut" + (minutes == 1 ? "" : "ter");
		} else if (minutes == 0) {
			return hours + " time" + (hours == 1 ? "" : "r");
		} else {
			return hours + " time" + (hours == 1 ? "" : "r") + " og " + minutes + " minut" + (minutes == 1 ? "" : "ter");
		}
	}
}
