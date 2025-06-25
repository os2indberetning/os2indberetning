package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.google.common.base.Stopwatch;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessAddressesTask {
	private final AddressService addressService;
	private final OS2indberetningConfiguration configuration;

	@Scheduled(cron = "${indberet.scheduled.processAddressesCron:0 */10 * ? * *}") // Every 10th minute
	public void processAddresses() {
		if (configuration.getScheduled().isEnabled()) {
			Stopwatch stopWatch = Stopwatch.createStarted();

			addressService.processUnprocessedAddresses();
			stopWatch.stop();

			if (stopWatch.elapsed(TimeUnit.MINUTES) > 1L) {
				log.info("Finished processing new addresses took: " + stopWatch.toString());
			}
		}
	}
}
