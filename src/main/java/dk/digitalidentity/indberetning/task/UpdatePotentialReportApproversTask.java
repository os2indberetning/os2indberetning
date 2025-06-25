package dk.digitalidentity.indberetning.task;

import com.google.common.base.Stopwatch;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdatePotentialReportApproversTask {
	private final OS2indberetningConfiguration configuration;
	private final ReportService reportService;

	@Scheduled(cron = "${indberet.scheduled.updateApproversForPendingReportsCron:0 #{new java.util.Random().nextInt(55)} 8,10,12,14,16 * * ?}")
	public void updateApproversForReports() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isUpdateApproversForPendingReportsEnabled()) {
			log.info("Updating potential approvers for all pending reports");
			Stopwatch stopWatch = Stopwatch.createStarted();

			reportService.updateApproversForPending();

			stopWatch.stop();
			if (stopWatch.elapsed(TimeUnit.MINUTES) > 1L) {
				log.info("Updating potential approvers for all pending reports took: " + stopWatch.toString());
			}
		}
	}
}
