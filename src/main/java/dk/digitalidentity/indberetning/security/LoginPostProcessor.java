package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class LoginPostProcessor implements SamlLoginPostProcessor {

	private final SecurityUtil securityUtil;
	private final PersonService personService;


	@Override
	public void process(TokenUser tokenUser) {
		Person person = personService.getByCpr(tokenUser.getUsername());
		securityUtil.updateTokenUser(tokenUser, person);
	}
}
