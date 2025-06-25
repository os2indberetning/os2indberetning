package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.entity.ApiTimeStamp;
import dk.digitalidentity.indberetning.service.ApiTimeStampService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckForMissingOpusData {
	private final OS2indberetningConfiguration configuration;
	private final ApiTimeStampService apiTimeStampService;

	// set to wednesday 14:XX
	@Scheduled(cron = "${indberet.scheduled.checkForOpusDataCron: 0  #{new java.util.Random().nextInt(55)} 14 ? * WED}")
	public void checkForMissingOpusData() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isCheckForOpusDataEnabled()) {
			log.info("Started task: checkForMissingOpusData");

			ApiTimeStamp latest = apiTimeStampService.find();
            if (latest != null && latest.getLastUpdated().isBefore(configuration.getDaysBeforeAlertingAboutOpus())) {
				log.error("No new opus data found in over {} latest: '{}'", configuration.getDaysBeforeAlertingAboutOpus(), latest.getLastUpdated());
            }

			log.info("Finished task: checkForMissingOpusData");
		}
	}
}
