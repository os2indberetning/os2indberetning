package dk.digitalidentity.indberetning.task;

import com.google.maps.internal.ratelimiter.Stopwatch;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.OnetimePaymentsCalculatorService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReCalculateReportsTask {
	private final OnetimePaymentsCalculatorService calculatorService;
	private final OS2indberetningConfiguration configuration;

	@Scheduled(cron = "${indberet.scheduled.recalculateReportsCron:#{new java.util.Random().nextInt(55)} 1/2 * * * ?}")
	public void reCalculateReports() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().isRecalculateReportsEnabled()) {
			return;
		}
		Stopwatch stopWatch = Stopwatch.createStarted();

		log.debug("Running recalculate service");
		calculatorService.recalculateReports();

		stopWatch.stop();
		if (stopWatch.elapsed(TimeUnit.MINUTES) > 1L) {
			log.info("Recalculating reports took: " + stopWatch.toString());
		}
	}
}
