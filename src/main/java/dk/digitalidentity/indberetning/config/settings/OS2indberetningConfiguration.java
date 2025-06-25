package dk.digitalidentity.indberetning.config.settings;

import dk.digitalidentity.indberetning.config.settings.modules.AddressWashEmailConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.DeadlineEmailConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ImportAPIConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.MapConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.OpusOnetimePaymentsConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.ScheduledConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indberet")
public class OS2indberetningConfiguration {
	private boolean developmentMode = false;
	private OpusOnetimePaymentsConfiguration opus = new OpusOnetimePaymentsConfiguration();
	private ImportAPIConfiguration api = new ImportAPIConfiguration();
	private ScheduledConfiguration scheduled = new ScheduledConfiguration();
	private MapConfiguration map = new MapConfiguration();
	private DeadlineEmailConfiguration email = new DeadlineEmailConfiguration();
	private AddressWashEmailConfiguration addressWash = new AddressWashEmailConfiguration();

	private int employmentCloseDelay = 14;
	private boolean allowNewReports = true;
	private boolean alertOnAppReportEditing = true;
	private boolean logAllAppReports = false;
	private LocalDate daysBeforeAlertingAboutOpus = LocalDate.now().minusDays(7);

}
