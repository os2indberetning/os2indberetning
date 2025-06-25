package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.samlmodule.config.SamlModuleConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UnknownUserFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;
    private final SamlModuleConfiguration samlLoginConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (securityUtil.isAuthenticated() && !securityUtil.hasRole(Roles.ROLE_USER) && !isNonSecuredPage(request)) {
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return "/error/unknown-user";
                }
            };
        }
        filterChain.doFilter(request, response);
    }

    private boolean isNonSecuredPage(HttpServletRequest request) {
        // This will exclude any background calls as well as saml calls

        String requestedPath = request.getRequestURI();
        if ("".equals(requestedPath) || "/".equals(requestedPath)) {
            return false; // This will show the error instead of the index page
        }

        if (requestedPath.startsWith("/saml")) {
            return true;
        }
        else if (this.samlLoginConfig.getIdp().isDiscovery() && requestedPath.equals(this.samlLoginConfig.getPages().getChooseIdentityProvider())) {
            return true;
        }
        else {
			return isConfiguredNonSecuredPage(requestedPath);
		}
	}

	private boolean isConfiguredNonSecuredPage(String requestedPath) {
		for (String page : this.samlLoginConfig.getPages().getNonsecured()) {
			if (page.endsWith("**") && requestedPath.startsWith(page.substring(0, page.length() - 2))) {
				return true;
			}

			if (page.startsWith("**") && requestedPath.endsWith(page.substring(2))) {
				return true;
			}

			if (requestedPath.equals(page)) {
				return true;
			}
		}
		return false;
	}
}