package dk.digitalidentity.indberetning.config.settings.modules;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressLookupServiceConfiguration {
	private String baseUrl = "https://adressevaelger.dk";
	private String apiKey = "adressevaelger123"; // Current default for everybody until they add actual access control
	private int maxRetryAttempts = 3;
	private Duration waitDuration = Duration.ofMillis(1500);

	private boolean logQueryParams = false;
}
