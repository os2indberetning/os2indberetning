package dk.digitalidentity.indberetning.config;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.filter.APISecurityFilter;
import dk.digitalidentity.indberetning.filter.InternalAPISecurityFilter;
import dk.digitalidentity.indberetning.filter.RouteDataAPISecurityFilter;
import dk.digitalidentity.indberetning.service.InternalAPIService;
import dk.digitalidentity.indberetning.service.RouteDataAPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class APISecurityFilterConfiguration {
	private final OS2indberetningConfiguration configuration;
	private final InternalAPIService internalAPIService;
	private final RouteDataAPIService routeDataAPIService;

	@Bean
	public FilterRegistrationBean<APISecurityFilter> coreDataApiSecurityFilter() {
		APISecurityFilter filter = new APISecurityFilter();
		filter.setConfiguration(configuration);

		FilterRegistrationBean<APISecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/*");

		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<InternalAPISecurityFilter> internalApiSecurityFilter() {
		InternalAPISecurityFilter filter = new InternalAPISecurityFilter();
		filter.setInternalAPIService(internalAPIService);

		FilterRegistrationBean<InternalAPISecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/internal/api/*");

		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<RouteDataAPISecurityFilter> routeDataApiSecurityFilter() {
		RouteDataAPISecurityFilter filter = new RouteDataAPISecurityFilter();
		filter.setRouteDataAPIService(routeDataAPIService);

		FilterRegistrationBean<RouteDataAPISecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/ext/api/routedata/*");

		return filterRegistrationBean;
	}
}
