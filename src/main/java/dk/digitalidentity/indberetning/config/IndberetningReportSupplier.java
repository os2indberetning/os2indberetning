package dk.digitalidentity.indberetning.config;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.reporter.suppliers.ReporterSupplier;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IndberetningReportSupplier implements ReporterSupplier {
	private final GitProperties gitProperties;

	private final OS2indberetningConfiguration configuration;

	@Override
	public Supplier<String> getCustomerNameSupplier() {
		return null;
	}

	@Override
	public Supplier<String> getProductNameSupplier() {
		return (() -> "OS2indberetning");
	}

	@Override
	public Supplier<String> getVersionSupplier() {
			return (() -> String.format("%s - %s", configuration.getVersion(), getShortCommitId()));
	}

	@Override
	public Supplier<Map<String, String>> getAttributeMapSupplier() {
		return (() -> Map.of("git_commit_id", getShortCommitId(),
							 "git_branch", getBranch()));
	}

	private String getBranch() {
		return gitProperties.getBranch() != null ? gitProperties.getBranch() : "N/A";
	}

	private String getShortCommitId() {
		return gitProperties.getShortCommitId() != null ? gitProperties.getShortCommitId() : "N/A";
	}
}
