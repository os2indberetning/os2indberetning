package dk.digitalidentity.indberetning.security;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DisallowNewReportingFilter extends OncePerRequestFilter {

    private final OS2indberetningConfiguration configuration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!configuration.isAllowNewReports() && isReportingEndpoint(request)) {
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return "/dashboard";
                }
            };
        }
        filterChain.doFilter(request, response);
    }

    private boolean isReportingEndpoint(HttpServletRequest request) {
        String requestedPath = request.getRequestURI();
        List<String> reportingEndpoints = List.of("/report", "/rest/report/create", "/rest/report/delete");
        return reportingEndpoints.contains(requestedPath);
    }
}