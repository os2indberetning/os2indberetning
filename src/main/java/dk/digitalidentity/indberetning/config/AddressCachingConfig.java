package dk.digitalidentity.indberetning.config;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.CacheConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.CacheConfiguration.CacheSpec;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AddressCachingConfig {
	public static final String COORDINATE_CACHE = "coordinateCache";
	public static final String ADDRESS_CACHE = "addressCache";

	private final OS2indberetningConfiguration configuration;

	@Bean
	public CacheManager addressCacheManager() {
		final CacheConfiguration cacheConfiguration = configuration.getCache();

		final CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		// pins the manager to these two names, so a @Cacheable naming an unknown cache fails instead of silently
		// getting a dynamically created, unconfigured one. must run before the custom caches are registered, since it
		// seeds the cache map with default-spec instances
		cacheManager.setCacheNames(List.of(COORDINATE_CACHE, ADDRESS_CACHE));
		cacheManager.registerCustomCache(COORDINATE_CACHE, build(cacheConfiguration.getCoordinateCache()));
		cacheManager.registerCustomCache(ADDRESS_CACHE, build(cacheConfiguration.getAddressCache()));

		return cacheManager;
	}

	private static Cache<Object, Object> build(final CacheSpec spec) {
		return Caffeine.newBuilder()
			.maximumSize(spec.getMaximumSize())
			.expireAfterWrite(spec.getExpireAfterWrite())
			.build();
	}
}
