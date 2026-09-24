package dk.digitalidentity.indberetning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dk.digitalidentity.indberetning.config.AddressCachingConfig;
import dk.digitalidentity.indberetning.config.CachingConfig;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.CacheConfiguration;

public class CacheManagerResolutionTest {
	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(
			OS2indberetningConfiguration.class,
			CachingConfig.class,
			AddressCachingConfig.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void unqualifiedCacheablesShouldResolveToTheUnboundedDefaultManager() {
		assertThat(context.getBean(CacheManager.class)).isInstanceOf(ConcurrentMapCacheManager.class);
	}

	@Test
	void addressCachesShouldResolveToTheCaffeineManager() {
		assertThat(context.getBean("addressCacheManager")).isInstanceOf(CaffeineCacheManager.class);
	}

	@Test
	void addressCacheManagerShouldExposeBothConfiguredCaches() {
		final CacheManager cacheManager = (CacheManager) context.getBean("addressCacheManager");

		assertThat(cacheManager.getCache(AddressCachingConfig.COORDINATE_CACHE)).isNotNull();
		assertThat(cacheManager.getCache(AddressCachingConfig.ADDRESS_CACHE)).isNotNull();
	}

	@Test
	void eachCacheShouldKeepItsOwnEvictionSpecRatherThanShareOne() {
		final CacheConfiguration cacheConfiguration = context.getBean(OS2indberetningConfiguration.class).getCache();

		assertThat(maximumSizeOf(AddressCachingConfig.COORDINATE_CACHE))
			.isEqualTo(cacheConfiguration.getCoordinateCache().getMaximumSize());
		assertThat(maximumSizeOf(AddressCachingConfig.ADDRESS_CACHE))
			.isEqualTo(cacheConfiguration.getAddressCache().getMaximumSize());
	}

	private long maximumSizeOf(final String cacheName) {
		final CacheManager cacheManager = (CacheManager) context.getBean("addressCacheManager");
		final CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);

		assertThat(cache).isNotNull();

		return cache.getNativeCache().policy().eviction().orElseThrow().getMaximum();
	}

	@Test
	void addressCacheManagerShouldNotCreateUnknownCachesOnDemand() {
		final CacheManager cacheManager = (CacheManager) context.getBean("addressCacheManager");

		assertThat(cacheManager.getCache("someCacheNobodyConfigured")).isNull();
	}
}
