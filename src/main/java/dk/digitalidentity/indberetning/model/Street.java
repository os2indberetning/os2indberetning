package dk.digitalidentity.indberetning.model;

import org.jspecify.annotations.NonNull;

public record Street(
	@NonNull String name,
	@NonNull String number
) {}
