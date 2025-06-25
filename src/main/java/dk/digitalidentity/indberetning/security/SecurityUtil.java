package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class SecurityUtil {
	public static final String EMAIL_ADDRESS_CLAIM_NAME = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
	private final HttpServletRequest request;
	private final PersonService personService;
	private final SubstituteService substituteService;
	private final EmploymentService employmentService;

	public SecurityUtil(HttpServletRequest request, PersonService personService, SubstituteService substituteService, EmploymentService employmentService) {
		this.request = request;
		this.personService = personService;
		this.substituteService = substituteService;
		this.employmentService = employmentService;
	}

	/**
	 * returns true if a user is currently logged in
	 */
	public boolean isAuthenticated() {
		return SecurityContextHolder.getContext().getAuthentication() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() != null &&
				SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
	}

	/**
	 * Returns true if the current {@link dk.digitalidentity.samlmodule.model.TokenUser} has the supplied role
	 */
	public boolean hasRole(String role) {
		if (!isAuthenticated()) {
			return false;
		}
		return getTokenUser().getAuthorities().stream().anyMatch(ga -> Objects.equals(ga.getAuthority().toUpperCase(), role));
	}

	/**
	 * returns the {@link dk.digitalidentity.samlmodule.model.TokenUser} stored on the session for the currently logged in user
	 */
	public TokenUser getTokenUser() {
		if (!isAuthenticated()) {
			return null;
		}

		return (TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails();
	}

	/**
	 * returns the ID of the currently logged in {@link Person}
	 */
	public String getPersonId() {
		if (!isAuthenticated()) {
			return null;
		}

		Object object = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (object instanceof Saml2AuthenticatedPrincipal samlPrincipal) {
			return samlPrincipal.getName();
		}

		return null;
	}

	/**
	 * Returns the user object of the currently logged in {@link Person}
	 *
	 * @return {@link Person} object or null if no user is logged in or there was an error fetching user by id from database
	 */
	@Nullable
	public Person getPerson() {
		String personId = getPersonId();
		if (!StringUtils.hasLength(personId)) {
			return null;
		}

		return personService.getByCpr(personId);
	}

//	public List<OrgUnit> getIsApproverForOus() {
//		if (!isAuthenticated() || !hasRole(Roles.ROLE_APPROVER)) {
//			return null;
//		}
//
//		Optional<SamlGrantedAuthority> any = getTokenUser().getAuthorities().stream()
//				.filter(ga -> Objects.equals(ga.getAuthority(), Roles.ROLE_APPROVER))
//				.findAny();
//
//		if (any.isPresent()) {
//			SamlGrantedAuthority authority = any.get();
//			if (authority.getConstraints() != null) {
//				Optional<SamlGrantedAuthority.Constraint> leaderOfConstraint = authority.getConstraints().stream()
//						.filter(constraint -> constraint.getConstraintType().equals("OU"))
//						.findAny();
//
//				return List.of()
//			}
//		}
//
//		return personService.getByCpr(personId);
//	}

	public String getName() {
		Person person = getPerson();
		if (person == null) {
			return "";
		}

		return person.getName();
	}

	/**
	 * returns true if the currently logged in person is an DBS admin
	 */
	public boolean isAdmin() {
		return hasRole(Roles.ROLE_ADMINISTRATOR);
	}

	/**
	 * Called during login, to grant access-roles to admin-portal
	 */
	public void updateTokenUser(TokenUser tokenUser, Person person) {
		List<SamlGrantedAuthority> authorities = new ArrayList<>();

		if (person != null) {
			authorities.add(new SamlGrantedAuthority(Roles.ROLE_USER));

			// TODO consider calculating which OUs and persons the user is an apporver for here
			//  so we dont have to do it often while the user nagivgates the application
			Set<Employment> employmentSet = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(person, LocalDate.now()));
			if (!employmentSet.isEmpty()) {
				authorities.add(new SamlGrantedAuthority(Roles.ROLE_APPROVER));
			}

			boolean admin = false;
			for (SamlGrantedAuthority authority : tokenUser.getAuthorities()) {
				if (Roles.ROLE_ADMINISTRATOR.equalsIgnoreCase(authority.getAuthority())) {
					authorities.add(new SamlGrantedAuthority(Roles.ROLE_ADMINISTRATOR));
					admin = true;
				}
			}

			tokenUser.setAuthorities(authorities);

			boolean updatePerson = false;
			Map<String, Object> attributes = tokenUser.getAttributes();
			if (attributes.containsKey(EMAIL_ADDRESS_CLAIM_NAME)) {
				Object emailAddressClaim = attributes.get(EMAIL_ADDRESS_CLAIM_NAME);
				if (!Objects.equals(person.getEmail(), emailAddressClaim) && StringUtils.hasLength(emailAddressClaim.toString())) {
					if (person.getEmail() == null) {
						// We get an email on this person. this means the flip to defaulting true for send email.
						// We still only send admin mail to people who also has isAdmin=true, and Approver mail to anyone who has someone they leadOrSub.
						person.setReceiveAdminMail(true);
						person.setReceiveApproverMail(true);
						person.setReceivePersonalMail(true);
					}

					person.setEmail(emailAddressClaim.toString());
					updatePerson = true;
				}
			}

			// Update isAdmin, this is only used when evaluating who should receive admin email.
			if (person.isAdmin() != admin) {
				person.setAdmin(admin);
				updatePerson = true;
			}

			if (updatePerson) {
				personService.save(person);
			}
		}

		// Spring really likes caching this object,
		// so we need to make sure Spring knows about the new version
		updateSecurityContext(tokenUser, authorities);
	}

	private void updateSecurityContext(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		if (SecurityContextHolder.getContext() != null &&
				SecurityContextHolder.getContext().getAuthentication() != null &&
				SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken) {

			SecurityContext securityContext = SecurityContextHolder.getContext();
			UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) securityContext.getAuthentication();

			authentication = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
			authentication.setDetails(tokenUser);

			SecurityContextHolder.getContext().setAuthentication(authentication);

			if (request != null) {
				HttpSession session = request.getSession(true);
				session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
			}
		}
	}

	public static String getUserIP() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return "0.0.0.0";
		}

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		return request.getRemoteAddr();
	}
}
