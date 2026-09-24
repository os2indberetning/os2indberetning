package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.security.enums.ClaimConstants;
import dk.digitalidentity.indberetning.security.enums.IdentityProviderType;
import dk.digitalidentity.indberetning.security.enums.MitidConstants;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class LoginPostProcessor implements SamlLoginPostProcessor {
	private final OS2indberetningConfiguration configuration;

	private final PersonService personService;
	private final EmploymentService employmentService;
	private final SubstituteService substituteService;

	@Override
	public void process(final TokenUser tokenUser) {
		List<SamlGrantedAuthority> authorities = new ArrayList<>();

		Person person = null;
		IdentityProviderType loginOrigin = null;
		if(configuration.getIdps().get(IdentityProviderType.MITID) != null && Objects.equals(tokenUser.getIssuer(), configuration.getIdps().get(IdentityProviderType.MITID).getEntityId())) {
			person = getPersonFromMitidPersonalLogin(tokenUser);
			loginOrigin = IdentityProviderType.MITID;
		} else if(configuration.getIdps().get(IdentityProviderType.NORMAL) != null && Objects.equals(tokenUser.getIssuer(), configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())) {
			person = personService.getByCpr(tokenUser.getUsername());
			loginOrigin = IdentityProviderType.NORMAL;
		} else {
			log.warn("Unknown issuer");
			clearSession();
			return;
		}

		if(person == null || loginOrigin == null) {
			log.warn("User was not found");
			clearSession();
			return;
		}

		authorities.add(new SamlGrantedAuthority(Roles.ROLE_USER));

		if(isPersonAnApprover(person)) {
			authorities.add(new SamlGrantedAuthority(Roles.ROLE_APPROVER));
		}

		// Only token users logging in through NORMAL IdP can get admin privileges 
		// This is because we get their admin authority externally through TokenUser authorities
		if(loginOrigin == IdentityProviderType.NORMAL)  {
			final boolean tokenUserAdmin = isTokenUserAnAdmin(tokenUser);
			// Update persons admin status
			if(tokenUserAdmin != person.isAdmin()) {
				person.setAdmin(tokenUserAdmin);
				personService.save(person);
			}

			if(tokenUserAdmin) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_ADMINISTRATOR));
			}

			handleEmailChange(person, tokenUser);
		}

		tokenUser.setAuthorities(authorities);

		HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		HttpSession session = req.getSession(true);
		session.setAttribute("loginOrigin", loginOrigin);
	}

	private void handleEmailChange(final Person person, final TokenUser tokenUser) {
		final String tokenUserEmail = (String)tokenUser.getAttributes().get(ClaimConstants.EMAIL_ADDRESS);
		if(shouldUpdatePersonEmail(person.getEmail(), tokenUserEmail)) {
			if(person.getEmail() == null)  {
				// We check if Person.isAdmin() is true before we send any admin emails
				// same thing applies to approvers
				person.setReceiveAdminMail(true);
				person.setReceiveApproverMail(true);
				person.setReceivePersonalMail(true);
			}
			person.setEmail(tokenUserEmail);
			personService.save(person);
		}
	}

	private Person getPersonFromMitidPersonalLogin(final TokenUser tokenUser) {
		final String cprAttrib = (String)tokenUser.getAttributes().get(MitidConstants.PERSONAL_CPR);
		if(!StringUtils.hasText(cprAttrib)) {
			return null;
		}

		return personService.getByCpr(cprAttrib);
	}

	private boolean isPersonAnApprover(final Person person) {
		final Set<Employment> employments = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(person, LocalDate.now()));
		return !employments.isEmpty();
	}

	private boolean isTokenUserAnAdmin(final TokenUser tokenUser) {
		return tokenUser.getAuthorities().stream()
			.anyMatch(authority -> Roles.ROLE_ADMINISTRATOR.equalsIgnoreCase(authority.getAuthority()));
	}

	private boolean shouldUpdatePersonEmail(final String oldEmail, final String emailClaim) {
		if(emailClaim == null || emailClaim.isBlank()) {
			return false;
		}

		return !Objects.equals(oldEmail, emailClaim);
	}

	private void clearSession() {
		SecurityContextHolder.clearContext();
		final HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		if (request != null) {
			final HttpSession session = request.getSession(false);
			if(session != null) {
				session.invalidate();
			}
		}
	}
}
