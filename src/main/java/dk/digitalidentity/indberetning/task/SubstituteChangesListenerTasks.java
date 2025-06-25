package dk.digitalidentity.indberetning.task;
import com.google.common.base.Stopwatch;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteChangesListenerTasks {
	private final OS2indberetningConfiguration configuration;
	private final SubstituteService substituteService;
	private final ReportService reportService;

	// Should run every 5 minutes
	@Scheduled(cron = "${indberet.scheduled.substituteChangeListenerCron:1 0/5 * * * *}")
	public void checkingSubstituteChanges() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isSubstituteChangeListenerEnabled()) {
			log.debug("Running task: checkingSubstituteChanges");
			if (substituteService.isChanged()) {
				Stopwatch stopWatch = Stopwatch.createStarted();

				log.info("SubstituteChangesListenerTasks: Changes have been made, running task");

				reportService.updateApproversForPending();
				stopWatch.stop();
				if (stopWatch.elapsed(TimeUnit.MINUTES) > 1L) {
					log.info("SubstituteChangesListenerTasks: Updating potential approvers for all pending reports took: " + stopWatch.toString());
				}
				else {
					log.debug("SubstituteChangesListenerTasks: Updating potential approvers for all pending reports took: " + stopWatch.toString());
				}
			}
			log.debug("Finished task: checkingSubstituteChanges");
		}
	}
}
