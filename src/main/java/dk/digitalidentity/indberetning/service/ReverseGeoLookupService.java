package dk.digitalidentity.indberetning.service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import dk.digitalidentity.indberetning.config.AddressCachingConfig;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ReverseGeoLookupConfiguration;
import dk.digitalidentity.indberetning.model.geometry.BBox;
import dk.digitalidentity.indberetning.model.geometry.CoordinateType;
import dk.digitalidentity.indberetning.model.geometry.Point;
import dk.digitalidentity.indberetning.service.dto.GeoAddressDTO;
import dk.digitalidentity.indberetning.service.exception.ReverseGeoLookupException;
import dk.digitalidentity.indberetning.service.exception.ReverseGeoLookupRetryableException;
import dk.digitalidentity.indberetning.util.CrsConverter;
import dk.digitalidentity.indberetning.util.GeometryUtils;
import dk.digitalidentity.indberetning.util.WKTParser;
import dk.digitalidentity.indberetning.util.exception.AddressParseException;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@CacheConfig(cacheManager = "addressCacheManager")
public class ReverseGeoLookupService {
	private final HttpSyncGraphQlClient graphqlClient;
	private final ReverseGeoLookupConfiguration configuration;
	private final RetryConfig retryConfig;

	public ReverseGeoLookupService(final OS2indberetningConfiguration configuration) {
		if(configuration.getReverseGeoLookup() == null) {
			throw new IllegalArgumentException("No configuration provided");
		}

		this.configuration = configuration.getReverseGeoLookup();

		if (!StringUtils.hasText(this.configuration.getApiKey())) {
			if (!configuration.isDevelopmentMode()) {
				throw new IllegalArgumentException("No datafordeler API key provided");
			}

			log.error("No datafordeler API key provided");
		}

		if (!StringUtils.hasText(this.configuration.getBaseUrl())) {
			throw new IllegalArgumentException("No datafordeler API base url provided");
		}

		final URI baseUrl = UriComponentsBuilder.fromUriString(this.configuration.getBaseUrl())
			.queryParam("apiKey", this.configuration.getApiKey())
			.build()
			.toUri();

		final RestClient restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.build();

		this.graphqlClient = HttpSyncGraphQlClient.create(restClient);


		this.retryConfig = RetryConfig.custom()
			.maxAttempts(this.configuration.getMaxRetryAttempts())
			.waitDuration(this.configuration.getWaitDuration())
			.retryExceptions(ReverseGeoLookupRetryableException.class)
			.failAfterMaxAttempts(true)
			.build();
	}

	@Cacheable(value = AddressCachingConfig.ADDRESS_CACHE, key = "T(com.github.davidmoten.geo.GeoHash).encodeHash(#point.lat(), #point.lon(), 9)")
	public GeoAddressDTO lookupClosestAddressToPoint(@NonNull final Point point) throws ReverseGeoLookupException {
		final Point pointUTM = CrsConverter.toUtm(point);

		final List<AddressPoint> addressPoints = lookupAddressPointsNearPoint(pointUTM);

		final List<AddressPoint> candidates = getClosestAddressPoints(addressPoints, pointUTM);
		if (candidates.isEmpty()) {
			throw new ReverseGeoLookupException("Ingen addresser fundet i nærheden af punktet");
		}

		final Map<String, HouseNumber> houseNumbers = lookupAddressesFromAddressPoints(candidates);

		// candidates are ordered by distance, so the first one carrying a house number is the closest usable address
		for (final AddressPoint candidate : candidates) {
			final HouseNumber houseNumber = houseNumbers.get(candidate.id_lokalId());
			if (houseNumber == null) {
				continue;
			}

			try {
				return new GeoAddressDTO(houseNumber.adgangsadressebetegnelse(), WKTParser.parsePoint(candidate.position().wkt(), CoordinateType.UTM));
			} catch (AddressParseException e) {
				log.warn("Could not parse address returned by datafordeleren: {}", houseNumber.adgangsadressebetegnelse());
			}
		}

		throw new ReverseGeoLookupException("Ingen addresser registeret ved punktet");
	}

	private @NonNull List<@NonNull AddressPoint> getClosestAddressPoints(@NonNull final List<@NonNull AddressPoint> nodes, @NonNull final Point target) {
		return nodes.stream()
			.filter(node -> node.position() != null && node.position().wkt() != null)
			.map(node -> Map.entry(node, GeometryUtils.distanceBetweenPoints(WKTParser.parsePoint(node.position().wkt(), CoordinateType.UTM), target)))
			.sorted(Map.Entry.comparingByValue())
			.limit(configuration.getMaxAddressPointCandidates())
			.map(Map.Entry::getKey)
			.toList();
	}

	record AddressPointPosition(
		String type,
		String wkt,
		int crs
	) {}

	record AddressPoint(
		String id_lokalId,
		AddressPointPosition position
	) {}

	private static final String ADDRESS_POINT_DOCUMENT = """
		query DAR_Adressepunkt($virkningstid: DafDateTime!, $searchArea: String!) {
			DAR_Adressepunkt(
				virkningstid: $virkningstid
				where: {
					position: {
						within: {
							wkt: $searchArea
							crs: 25832
						}
					}
				}
			) {
				nodes {
					id_lokalId
					position {
						type
						wkt
						crs
					}
				}
			}
		}
		""";

	private @NonNull List<@NonNull AddressPoint> lookupAddressPointsNearPoint(@NonNull final Point point) throws ReverseGeoLookupException {
		final BBox searchArea = new BBox(point, configuration.getSearchAreaSize());

		final String virkningstid = Instant.now().truncatedTo(ChronoUnit.MICROS).toString();

		final var retryableAddressPoints = Retry.decorateSupplier(
			Retry.of("ReversegeoLookupService::lookupAddressPointsNearPoint", retryConfig),
			() -> {
				final ClientGraphQlResponse resp = graphqlClient.document(ADDRESS_POINT_DOCUMENT)
					.variable("virkningstid", virkningstid)
					.variable("searchArea", searchArea.toWKTPolygon())
					.executeSync();

				if(!resp.isValid()) {
					log.warn("DAR_Adressepunkt query failed with errors: {}", resp.getErrors());
					throw new ReverseGeoLookupRetryableException();
				}

				final var field = resp.field("DAR_Adressepunkt.nodes");
				if(field == null) {
					log.warn("DAR_Adressepunkt.nodes query response was empty");
					throw new ReverseGeoLookupRetryableException();
				}

				return field.toEntityList(AddressPoint.class);
			}
		);

		try {
			final List<AddressPoint> addressPoints = retryableAddressPoints.get();
			if (addressPoints.isEmpty()) {
				throw new ReverseGeoLookupException("Ingen addresser fundet i nærheden af punktet");
			}

			return addressPoints;
		} catch(MaxRetriesExceededException | ReverseGeoLookupRetryableException e) {
			throw new ReverseGeoLookupException("Datafordeleren er utilgængelig");
		}
	}


	record HouseNumber(
		String id_lokalId,
		String adgangsadressebetegnelse
	) {}

	private static final String HOUSE_NUMBER_DOCUMENT = """
		query DAR_Husnummer($virkningstid: DafDateTime!, $lokalIds: [String!]!) {
			DAR_Husnummer(
				virkningstid: $virkningstid
				where: {
					id_lokalId: { in: $lokalIds }
				}
			) {
				nodes {
					id_lokalId
					adgangsadressebetegnelse
				}
			}
		}
		""";

	private @NonNull Map<String, HouseNumber> lookupAddressesFromAddressPoints(@NonNull final List<@NonNull AddressPoint> addressPoints) throws ReverseGeoLookupException {
		final String virkningstid = Instant.now().truncatedTo(ChronoUnit.MICROS).toString();
		final List<String> lokalIds = addressPoints.stream().map(AddressPoint::id_lokalId).toList();

		final var retryableHouseNumbers = Retry.decorateSupplier(
			Retry.of("ReversegeoLookupService::lookupAddressesFromAddressPoints", retryConfig),
			() -> {
				final ClientGraphQlResponse resp = graphqlClient.document(HOUSE_NUMBER_DOCUMENT)
					.variable("virkningstid", virkningstid)
					.variable("lokalIds", lokalIds)
					.executeSync();

				if (!resp.isValid()) {
					log.warn("DAR_Husnummer query failed with errors: {}", resp.getErrors());
					throw new ReverseGeoLookupRetryableException();
				}

				final var field = resp.field("DAR_Husnummer.nodes");
				if(field == null) {
					log.warn("DAR_Husnummer query response was empty");
					throw new ReverseGeoLookupRetryableException();
				}
				
				return field.toEntityList(HouseNumber.class);
			}
		);

		try {
			return retryableHouseNumbers.get().stream()
				.collect(Collectors.toMap(HouseNumber::id_lokalId, Function.identity(), (first, second) -> first));
		} catch(MaxRetriesExceededException | ReverseGeoLookupRetryableException e) {
			throw new ReverseGeoLookupException("Datafordeleren er utilgængelig");
		}
	}
}
