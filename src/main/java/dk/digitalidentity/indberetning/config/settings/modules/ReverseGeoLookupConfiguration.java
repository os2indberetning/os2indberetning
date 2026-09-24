package dk.digitalidentity.indberetning.config.settings.modules;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReverseGeoLookupConfiguration {
	private String baseUrl = "https://graphql.datafordeler.dk/DAR/v3";
	private String apiKey;
	// half-width in metres: 250 yields a 500m x 500m search box centered on the point
	private double searchAreaSize = 250.0;
	// how many of the nearest address points to resolve house numbers for, since the nearest may not have one
	private int maxAddressPointCandidates = 5;
	private int maxRetryAttempts = 3;
	private Duration waitDuration = Duration.ofMillis(1500);
}
