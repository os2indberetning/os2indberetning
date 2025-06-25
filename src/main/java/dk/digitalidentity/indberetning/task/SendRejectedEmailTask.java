package dk.digitalidentity.indberetning.task;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendRejectedEmailTask {
	private final OS2indberetningConfiguration configuration;
	private final ReportService reportService;

	// Should run at 8 am every morning
	@Scheduled(cron = "${indberet.scheduled.sendRejectedEmailsCron:0 0 8 * * ?}")
	public void sendRejectedEmails() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isSendRejectedEmailsEnabled()) {
			log.info("Running task: sendRejectedEmails");
			reportService.sendRejectedEmails();
			log.info("Finished task: sendRejectedEmails");
		}
	}
}
