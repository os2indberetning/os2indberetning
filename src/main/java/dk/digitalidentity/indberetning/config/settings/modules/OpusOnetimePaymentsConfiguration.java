package dk.digitalidentity.indberetning.config.settings.modules;

import dk.digitalidentity.indberetning.service.BaseURLUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class OpusOnetimePaymentsConfiguration {
	private String keystoreLocation;
	private String keystorePassword;
	private String municipalityCode;
	private String apiBaseUrl;
	private boolean onlyValidateUpdates = false;
	private boolean correctInvalidOnetimePayments = true;

	public String getApiBaseUrl() {
		return BaseURLUtil.getBaseURL(apiBaseUrl);
	}
}
