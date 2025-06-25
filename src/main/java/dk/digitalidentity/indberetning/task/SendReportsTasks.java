package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.OnetimePaymentsExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SendReportsTasks {
	private final OS2indberetningConfiguration configuration;
	private final OnetimePaymentsExportService exportService;

	@Scheduled(cron = "${indberet.scheduled.exportOnetimePaymentsCron:0 #{new java.util.Random().nextInt(55)} 4 * * ?}")
	public void sendReports() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().isExportOnetimePaymentsEnabled()) {
			return;
		}

		log.info("Running sendReports task");
		exportService.sendReports();
	}
}
