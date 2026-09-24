package dk.digitalidentity.indberetning.config.settings;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.indberetning.config.settings.modules.AddressLookupServiceConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.AddressWashEmailConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.CacheConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ReverseGeoLookupConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.DeadlineEmailConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.IdentityProviderConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ImportAPIConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.MapConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.OpusOnetimePaymentsConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ScheduledConfiguration;
import dk.digitalidentity.indberetning.security.enums.IdentityProviderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indberet")
public class OS2indberetningConfiguration {
	private final String version = "2026r2";
	private boolean developmentMode = false;
	private OpusOnetimePaymentsConfiguration opus = new OpusOnetimePaymentsConfiguration();
	private ImportAPIConfiguration api = new ImportAPIConfiguration();
	private ScheduledConfiguration scheduled = new ScheduledConfiguration();
	private MapConfiguration map = new MapConfiguration();
	private DeadlineEmailConfiguration email = new DeadlineEmailConfiguration();
	private AddressWashEmailConfiguration addressWash = new AddressWashEmailConfiguration();
    private Map<IdentityProviderType, IdentityProviderConfiguration> idps;
	private ReverseGeoLookupConfiguration reverseGeoLookup = new ReverseGeoLookupConfiguration();
	private AddressLookupServiceConfiguration addressLookup = new AddressLookupServiceConfiguration();
	private CacheConfiguration cache = new CacheConfiguration();

	private int employmentCloseDelay = 14;
	private boolean allowNewReports = true;
	private boolean alertOnAppReportEditing = true;
	private boolean enableCreationTimeStampOnGps = false;
	private boolean logAllAppReports = false;
	private LocalDate daysBeforeAlertingAboutOpus = LocalDate.now().minusDays(7);
	private boolean showExcludedOrgUnitsInUi = false;
	private boolean allowTimePickerAndTimeEstimation = false;

	public boolean skipDiscovery() {
		return idps.size() <= 1;
	}
}
