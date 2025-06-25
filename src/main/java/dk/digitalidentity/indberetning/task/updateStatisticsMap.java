package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class updateStatisticsMap {
	private final OS2indberetningConfiguration configuration;
	private final StatisticService statisticService;

	@Scheduled(cron = "${indberet.scheduled.updateStatisticsCron:#{new java.util.Random().nextInt(55)} 1/5 * * * ?}")
	public void updateStatistics() {
		if (!configuration.getScheduled().isUpdateStatisticsEnabled()) {
			return;
		}
		log.debug("Running task: updateStatistics");
		statisticService.updateStatistics();
		log.debug("Finished task: updateStatistics");
	}
}
