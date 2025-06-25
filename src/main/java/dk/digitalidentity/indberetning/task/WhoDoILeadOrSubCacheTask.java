package dk.digitalidentity.indberetning.task;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhoDoILeadOrSubCacheTask {
	private final SubstituteService substituteService;
	private final AddressService addressService;
	private final OS2indberetningConfiguration configuration;

	@Scheduled(fixedRate = 5 * 60 * 1000L) // Every 5 minutes
	public void clearWhoDoILeadOrSubCache() {
		substituteService.clearCache();
		addressService.clearGetHomeAddressByDateCache();
	}
}
