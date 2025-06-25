package dk.digitalidentity.indberetning.task;

import com.google.maps.internal.ratelimiter.Stopwatch;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogCleanupTask {
	private final AuditLogService auditLogService;
	private final OS2indberetningConfiguration configuration;

	@Scheduled(cron = "${indberet.scheduled.cleanupOldAuditlogCron:0 #{new java.util.Random().nextInt(55)} 14 ? * WED}")
	public void cleanUpOldAuditLogs() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().isCleanupOldAuditlogEnabled()) {
			return;
		}

		Stopwatch stopWatch = Stopwatch.createStarted();
		log.debug("Starting task: cleanUpOldAuditLogs");

		auditLogService.cleanUpOldAuditLogs();
		auditLogService.deleteUnreferencedAuditlogDetails();

		stopWatch.stop();
		log.debug("Finished task: cleanUpOldAuditLogs");

		if (stopWatch.elapsed(TimeUnit.MINUTES) > 1L) {
			log.info("cleanUpOldAuditLogs took: " + stopWatch.toString());
		}
	}
}
