package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.emailService.service.EmailService;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.DeadlineEmailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineEmailsNotificationTask {
	private final DeadlineEmailsService deadlineEmailsService;
	private final OS2indberetningConfiguration configuration;
	private final EmailService emailService;

	// Runs every day at 00:xx, 04:xx, 08:xx, 12:xx, 16:xx, 20:xx
	@Scheduled( cron = "${indberet.scheduled.deadlineEmailsCron:0 #{new java.util.Random().nextInt(55)} 0/4 * * ?}")
	public void processDeadlines() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isDeadlineEmailsEnabled()) {
			log.info("Processing deadline emails and adding to email queue");

			// Emails are sent out every 5 minutes between 7 and 17
			// but we only need to process deadline emails a couple of times a day
			deadlineEmailsService.processDeadlineEmails();
		}
	}

	// Runs every 7th day at noon
	@Scheduled( cron = "${indberet.scheduled.addressWashEmailsCron:0 #{new java.util.Random().nextInt(55)} 12 * * MON}")
	public void sendEmailsForAddressWash() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isAddressWashEmailsEnabled()) {
			log.info("Processing emails for notifications to address wash");

			deadlineEmailsService.sendEmailsToAdmins(configuration.getAddressWash().getDefaultSubject(), configuration.getAddressWash().getDefaultMessage());

			log.info("Done processing emails for notifications to address wash");
		}
	}
}
