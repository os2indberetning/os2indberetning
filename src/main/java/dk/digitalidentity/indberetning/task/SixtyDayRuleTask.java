package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.SixtyDayRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class SixtyDayRuleTask {

	private final SixtyDayRuleService sixtyDayRuleService;
	private final OS2indberetningConfiguration configuration;

	// Weekly. Between 18:00:00-18:55:00 every Thursday
	@Scheduled(cron = "${indberet.scheduled.sixtyDayRuleCron:0 #{new java.util.Random().nextInt(55)} 18 * * THU}")
	public void check60DaysRule() {
		if (configuration.getScheduled().isEnabled() && configuration.getScheduled().isSixtyDayRuleEnabled()) {
			log.info("Running task: check 60 days rule");
			sixtyDayRuleService.check60Days();
		}
	}

	// Weekly. Between 17:00:00-17:55:00 every Thursday
	@Scheduled(cron = "${indberet.scheduled.sixtyDayRuleResetCron:0 #{new java.util.Random().nextInt(55)} 17 * * THU}")
	public void checkAllFor60DaysReset() {
		if (configuration.getScheduled().isEnabled()) {
			log.info("Running task: check all for 60 days reset");
			sixtyDayRuleService.checkAllFor60DaysReset();
		}
	}
}
