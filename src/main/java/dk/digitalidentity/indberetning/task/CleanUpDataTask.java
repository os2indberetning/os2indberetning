package dk.digitalidentity.indberetning.task;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.CleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanUpDataTask {
    private final OS2indberetningConfiguration configuration;
	private final CleanupService cleanupService;

	// Runs once at midnight
    @Scheduled(cron = "${indberet.scheduled.cleanUpOldDataCron:0 #{new java.util.Random().nextInt(55)} 5 * * ?}")
    public void cleanUpOldData() {
        if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().isCleanUpOldDataEnabled()) {
            log.debug("Skipping task: cleanUpOldData, scheduled disabled or not enabled in configuration.properties.");
			return;
		}

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

        log.info("Running task: cleanUpOldData");
        cleanupService.cleanup(configuration.getScheduled().isCleanUpTaskDryRun());

        stopWatch.stop();
		if (stopWatch.getTotalTimeSeconds() > 5) {
			log.info("Task: cleanUpOldData took: " + stopWatch.shortSummary());
		}
	}
}
