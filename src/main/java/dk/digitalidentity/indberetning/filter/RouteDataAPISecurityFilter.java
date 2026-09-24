package dk.digitalidentity.indberetning.filter;

import dk.digitalidentity.indberetning.service.RouteDataAPIService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
public class RouteDataAPISecurityFilter extends OncePerRequestFilter {

	private RouteDataAPIService routeDataAPIService;

	public void setRouteDataAPIService(RouteDataAPIService routeDataAPIService) {
		this.routeDataAPIService = routeDataAPIService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		// we are using a custom header instead of Authorization because the Authorization header plays very badly with the SAML filter
		String authHeader = request.getHeader("ApiKey");
		if (authHeader != null) {
			try {
				if (!Objects.equals(routeDataAPIService.getApiKey(), authHeader)) {
					unauthorized(response, "Invalid ApiKey header", authHeader);
					return;
				}
			} catch (NoSuchElementException ex) {
				unauthorized(response, "Invalid ApiKey header", authHeader);
				return;
			}

			filterChain.doFilter(request, response);
		} else {
			unauthorized(response, "Missing ApiKey header", authHeader);
		}
	}

	private static void unauthorized(HttpServletResponse response, String message, String authHeader) throws IOException {
		log.warn(message + " (authHeader = " + authHeader + ")");
		response.sendError(401, message);
	}
}
