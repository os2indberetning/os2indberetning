package dk.digitalidentity.indberetning.service;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import dk.digitalidentity.indberetning.config.AddressCachingConfig;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.AddressLookupServiceConfiguration;
import dk.digitalidentity.indberetning.exceptions.AddressLookupRuntimeException;
import dk.digitalidentity.indberetning.model.geometry.CoordinateType;
import dk.digitalidentity.indberetning.model.geometry.Point;
import dk.digitalidentity.indberetning.service.dto.AddressLookupDTO;
import dk.digitalidentity.indberetning.service.exception.AddressLookupRuntimeRetryableException;
import dk.digitalidentity.indberetning.util.CrsConverter;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@CacheConfig(cacheManager = "addressCacheManager")
public class AddressLookupService {
	private final AddressLookupServiceConfiguration configuration;
	private final RestClient restClient;
	private final RetryConfig retryConfig;

	AddressLookupService(@NonNull final OS2indberetningConfiguration configuration) {
		if (configuration.getAddressLookup() == null) {
			throw new IllegalArgumentException("No configuration provided");
		}

		this.configuration = configuration.getAddressLookup();

		if (!StringUtils.hasText(this.configuration.getApiKey())) {
			if (!configuration.isDevelopmentMode()) {
				throw new IllegalArgumentException("No datafordeler adressevaelger API key provided");
			}

			log.error("No datafordeler adressevaelger API key provided");
		}

		if (!StringUtils.hasText(this.configuration.getBaseUrl())) {
			throw new IllegalArgumentException("No datafordeler addressevaelger API base url provided");
		}

		this.restClient = RestClient.builder()
			.baseUrl(this.configuration.getBaseUrl())
			.build();

		this.retryConfig = RetryConfig.custom()
			.maxAttempts(this.configuration.getMaxRetryAttempts())
			.waitDuration(this.configuration.getWaitDuration())
			.retryExceptions(AddressLookupRuntimeRetryableException.class)
			.failAfterMaxAttempts(true)
			.build();
	}

	record CRSProperties(String name) {}
	record CRS(CRSProperties properties) {}
	record Geometry(CRS crs, List<Double> coordinates) {}
	record AccessPoint(Geometry geometri) {}
	record HouseNumber(AccessPoint adgangspunkt) {}
	record HouseNumberResponse(HouseNumber husnummer) {
		@Nullable Point getPoint() {
			if (husnummer == null
				|| husnummer.adgangspunkt == null
				|| husnummer.adgangspunkt.geometri == null) {
				return null;
			}

			final Geometry geometri = husnummer.adgangspunkt.geometri;
			if (geometri.coordinates == null || geometri.coordinates.size() < 2) {
				return null;
			}

			if (geometri.crs == null || geometri.crs.properties == null) {
				return null;
			}

			if(!"EPSG:25832".equals(geometri.crs.properties.name)) {
				log.warn("Unsupported coordinate system: {}", geometri.crs.properties.name);
				return null;
			}

			final List<Double> coords = geometri.coordinates;

			return new Point(coords.get(1), coords.get(0), CoordinateType.UTM);
		}
	}

	/**
	 *
	 * @throws AddressLookupRuntimeException
	 */
	@Cacheable(AddressCachingConfig.COORDINATE_CACHE)
	public @NonNull Point lookupCoordinatesForAddress(@NonNull final AddressLookupDTO address) throws AddressLookupRuntimeException {
		final Supplier<HouseNumberId> retryableId = Retry.decorateSupplier(
			Retry.of("AddressLookupService::getHouseNumberId", retryConfig),
			() -> {
				return getHouseNumberId(address);
			}
		);

		final HouseNumberId id;
		try {
			id = retryableId.get();
		} catch (MaxRetriesExceededException e) {
			log.warn("house number id lookup exhausted", e);
			throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig efter gentagne forsøg");
		}

		final Supplier<HouseNumberResponse> retryableHouseNumber = Retry.decorateSupplier(
			Retry.of("AddressLookupService::getHouseNumber", retryConfig),
			() -> {
				return getHouseNumber(id);
			}
		);

		final HouseNumberResponse houseNumber;
		try  {
			houseNumber = retryableHouseNumber.get();
		} catch (MaxRetriesExceededException e) {
			log.warn("house number lookup exhausted", e);
			throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig efter gentagne forsøg");
		}

		final Point point = houseNumber.getPoint();
		if(point == null) {
			throw new AddressLookupRuntimeException("Kunne ikke finde koordinaterne på addressen");
		}

		return CrsConverter.toWgs84(point);
	}

	private @NonNull HouseNumberResponse getHouseNumber(@NonNull final HouseNumberId id) {
		final HouseNumberResponse resp = restClient.get()
			.uri(builder -> builder
				.path("/husnumre/" + id.id)
				.queryParam("token", configuration.getApiKey())
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				if(configuration.isLogQueryParams()) {
					log.warn("House number query params: id = {}", id.id);
				}

				log.warn("Failed to get house number. Got status code: {} and status text: {}", String.valueOf(res.getStatusCode()).toLowerCase(), new String(res.getBody().readAllBytes()));
				throw new AddressLookupRuntimeRetryableException("Dataforsyningens kortservice er utilgængelig");
			})
			.body(HouseNumberResponse.class);

		if (resp == null) {
			throw new AddressLookupRuntimeException("Kunne ikke finde husnummeret på addressen");
		}

		return resp;
	}

	record HouseNumberId(String type, String id) {}
	record HouseNumberIdResponse(List<HouseNumberId> fund) {}
	private @NonNull HouseNumberId getHouseNumberId(@NonNull final AddressLookupDTO address) {
		final HouseNumberIdResponse resp = restClient.get()
			.uri(builder -> builder
				.path("/soeg")
				.queryParam("vejnavn", address.streetName())
				.queryParam("husnummer", address.streetNumber())
				.queryParam("postnummer", address.postalCode())
				.queryParam("token", configuration.getApiKey())
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				if(configuration.isLogQueryParams()) {
					log.warn("House number id query params: vejnavn = {}, husnummer = {}, postnummer = {}", address.streetName(), address.streetNumber(), address.postalCode());
				}

				log.warn("Failed to get house number ID. Got status code: {} and status body: {}", String.valueOf(res.getStatusCode()).toLowerCase(), new String(res.getBody().readAllBytes()));
				throw new AddressLookupRuntimeRetryableException("Dataforsyningens kortservice er utilgængelig");
			})
			.body(HouseNumberIdResponse.class);

		if(resp == null) {
			throw new AddressLookupRuntimeException("Kunne ikke finde addressen");
		}

		if(resp.fund == null || resp.fund.isEmpty() || resp.fund.getFirst() == null) {
			throw new AddressLookupRuntimeException("Intet husnummer registeret ved addressen");
		}

		return resp.fund.getFirst();
	}
}
