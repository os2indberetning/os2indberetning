package dk.digitalidentity.indberetning.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// The SAML module controls the SecurityFilterChain and does not expose header customization,
// so we cannot configure the Referrer-Policy via Spring Security's HeadersConfigurer directly.
// This filter overrides the default "no-referrer" policy to "strict-origin-when-cross-origin",
// which is required by OpenStreetMap's tile usage policy: map tile requests must include a Referer
// header so OSM can identify the application making the requests.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReferrerPolicyOverrideFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                if ("Referrer-Policy".equalsIgnoreCase(name)) {
                    super.setHeader(name, "strict-origin-when-cross-origin");
                } else {
                    super.setHeader(name, value);
                }
            }

            @Override
            public void addHeader(String name, String value) {
                if ("Referrer-Policy".equalsIgnoreCase(name)) {
                    super.setHeader(name, "strict-origin-when-cross-origin");
                } else {
                    super.addHeader(name, value);
                }
            }
        });
    }
}
