package dk.digitalidentity.indberetning.model;

import org.jspecify.annotations.NonNull;

public record ZipCode(
	@NonNull String code,
	@NonNull String district
) {}
