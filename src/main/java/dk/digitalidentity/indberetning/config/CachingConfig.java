package dk.digitalidentity.indberetning.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableCaching
@Configuration
public class CachingConfig {

	/**
	 * Default manager for caches that do not opt into a dedicated one. Declaring any CacheManager bean makes Spring
	 * Boot's auto-configured one back off, so this has to be declared explicitly to keep unqualified @Cacheable methods
	 * on the unbounded, non-expiring behaviour they were written against.
	 */
	@Bean
	@Primary
	public CacheManager defaultCacheManager() {
		return new ConcurrentMapCacheManager();
	}
}
