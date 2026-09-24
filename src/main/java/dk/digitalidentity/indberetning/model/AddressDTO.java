package dk.digitalidentity.indberetning.model;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

public record AddressDTO(
	@NonNull Street street,
	Optional<String> town,
	@NonNull ZipCode zipCode
) {}
