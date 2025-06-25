package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CheckForTerminationsTask {
	private final OS2indberetningConfiguration configuration;
	private final SubstituteService substituteService;

	@Scheduled(cron = "${indberet.scheduled.checkForTerminationsTaskCron:0 0 6 * * ?}")
	public void checkForTerminationsTask() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().isCheckForTerminationsTaskEnabled()) {
			return;
		}

		log.info("Starting task: checkForTerminations");

		substituteService.checkForTerminations();

		log.info("Finished task: checkForTerminations");
	}
}
