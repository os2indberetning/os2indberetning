package dk.digitalidentity.indberetning.config.settings.modules;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.convert.DurationUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CacheConfiguration {
	// address -> coordinate lookups against the datafordeler address API. keyed by address, so the key space is bounded
	// by the addresses actually in use and reuse is high
	private CacheSpec coordinateCache = new CacheSpec(2000, Duration.ofHours(24));

	// coordinate -> address reverse lookups against the datafordeler DAR API. keyed by a ~5m geohash cell derived from
	// gps points, so the key space grows with driven distance rather than with the number of distinct addresses
	private CacheSpec addressCache = new CacheSpec(5000, Duration.ofHours(24));

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CacheSpec {
		private long maximumSize;

		// DAR data is near static, so entries are kept for a long time. a suffix-less value is read as minutes
		@DurationUnit(ChronoUnit.MINUTES)
		private Duration expireAfterWrite;
	}
}
