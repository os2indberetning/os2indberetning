package dk.digitalidentity.indberetning.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.indberetning.exceptions.AddressLookupRuntimeException;
import dk.digitalidentity.indberetning.model.geometry.CoordinateType;
import dk.digitalidentity.indberetning.model.geometry.Point;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.AddressLookupService;
import dk.digitalidentity.indberetning.service.ReverseGeoLookupService;
import dk.digitalidentity.indberetning.service.RouteService;
import dk.digitalidentity.indberetning.service.dto.AddressLookupDTO;
import dk.digitalidentity.indberetning.service.exception.ReverseGeoLookupException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class RouteAPIController {
    private final RouteService routeService;
	private final ReverseGeoLookupService reverseGeoLookupService;
	private final AddressLookupService addressLookupService;

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Void> handleValidationException(ConstraintViolationException ex) {
		return ResponseEntity.badRequest().build();
	}

    @GetMapping("/routeapi/addressToLatLng/{road},{house_number},{postcode}")
    @Operation(
            summary = "Address To LatLng convert",
            description = "Gets the coordinates of an address to convert",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adresse konverteret"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "400", description = "Could not delete deviating address")
            }
    )
    public ResponseEntity<?> addressToLatLng(@PathVariable @NotBlank String road, @PathVariable @NotBlank String house_number, @PathVariable @NotBlank @Pattern(regexp = "\\d{4}") String postcode) {
        try {
			return ResponseEntity.ok(addressLookupService.lookupCoordinatesForAddress(new AddressLookupDTO(road, house_number, postcode)));
        }
        catch (AddressLookupRuntimeException e) {
			log.warn("Failed to get coordinates of an address", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (Exception e) {
			log.error("Failed to get coordinates of an address", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/routeapi/latLngToAddress/{lat},{lng}")
    @Operation(
            summary = "Latitude, Longtitute to address conversion",
            description = "Used to map coordinates to an address",
            responses = {
                    @ApiResponse(responseCode = "200", description = "koordinater konverteret!"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "400", description = "Could not delete deviating address")
            }
    )
    public ResponseEntity<?> latLngToAddress(@PathVariable double lat,  @PathVariable double lng) {
        try {
            return ResponseEntity.ok(reverseGeoLookupService.lookupClosestAddressToPoint(new Point(lat, lng, CoordinateType.WGS84)));
        }
        catch (ReverseGeoLookupException e) {
			log.warn("Failed to get address for coordinates", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (Exception e) {
			log.error("Failed to get address for coordinates", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public record Error(String code, String message) {}
    public record CoordsRec (String coordinates, String routeCalculationPreference, String functionName) {}
    @PostMapping("/routeapi/route/")
	@Operation(
			summary = "Get route, given coordinate strings and calculation-preference",
			description = "Used to retrieve a route between coordinates",
			responses = {
					@ApiResponse(responseCode = "200", description = "Rute dannet"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
    public ResponseEntity<?> route(@RequestBody CoordsRec coords) throws JsonProcessingException {
		try {
			return ResponseEntity.ok(routeService.getRoute(coords.coordinates, coords.functionName));
		}
		catch (HttpClientErrorException ex) {
			log.warn(ex.getResponseBodyAsString());

			ObjectMapper objectMapper = new ObjectMapper();

			JsonNode rootNode = objectMapper.readTree(ex.getResponseBodyAsString());
			JsonNode statusCodeNode = rootNode.findValue("code");
			JsonNode messageNode = rootNode.findValue("message");

			String statusCode = "";
			if (statusCodeNode != null) {
				statusCode = statusCodeNode.asText();
			}

			String message = "";
			if (messageNode != null) {
				message = messageNode.asText();
			}

			return ResponseEntity.badRequest().body(new Error(statusCode, message));
		}
	}
}
