package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.security.enums.IdentityProviderType;
import dk.digitalidentity.indberetning.security.enums.MitidConstants;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SecurityUtil {
	private final PersonService personService;

	public SecurityUtil(PersonService personService) {
		this.personService = personService;
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
			HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			HttpSession session = req.getSession(false); 
			if(session == null) {
				throw new IllegalStateException("Session not found");
			}

			IdentityProviderType loginOrigin = (IdentityProviderType)session.getAttribute("loginOrigin");
			if(loginOrigin == null) {
				throw new IllegalStateException("Login origin not found");
			}

			if(loginOrigin.equals(IdentityProviderType.MITID)) {
				List<Object> cprAttrib = samlPrincipal.getAttribute(MitidConstants.PERSONAL_CPR);
				if(cprAttrib == null) {
					return null;
				}

				return (String)cprAttrib.getFirst();
			}
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

	public static String getUserIP() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return "0.0.0.0";
		}

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		return request.getRemoteAddr();
	}
}
