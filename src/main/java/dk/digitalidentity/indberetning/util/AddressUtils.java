package dk.digitalidentity.indberetning.util;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;

import dk.digitalidentity.indberetning.model.AddressDTO;
import dk.digitalidentity.indberetning.model.Street;
import dk.digitalidentity.indberetning.model.ZipCode;
import dk.digitalidentity.indberetning.util.exception.AddressParseException;

public class AddressUtils {
	private static final Pattern DESIGNATION = Pattern.compile(
		"^(\\d+\\.|st\\.|kld\\.|kl\\.)(\\s+(tv\\.|th\\.|mf\\.|\\d+))?$",
		Pattern.CASE_INSENSITIVE);

	/**
	 * Splits an address based on <a href="https://danmarksadresser.dk/om-adresser/saadan-gengives-en-adresse">Danmarks Addresser</a>
	 * @throws AddressParseException 
	 */
	public static @NonNull AddressDTO parseAddress(@NonNull final String fullAddress) throws AddressParseException {
		final String[] parts = fullAddress.split(", ");

		if(parts.length < 2 || parts.length > 4) {
			throw new AddressParseException("Invalid address: " + fullAddress);
		}

		final Street street = parseStreet(parts[0]);
		final Optional<String> town = parseTown(parts);
		final ZipCode zipCode = parseZipCode(parts[parts.length - 1]);

		return new AddressDTO(street, town, zipCode);
	}

	private static @NonNull Street parseStreet(@NonNull final String street) throws AddressParseException {
		final int streetNameDelimiterIdx = street.lastIndexOf(" ");
		if (streetNameDelimiterIdx < 0) {
			throw new AddressParseException("Expected street name and street number to be space separated, got: " + street);
		}

		final String streetName = street.substring(0, streetNameDelimiterIdx);
		final String streetNumber = street.substring(streetNameDelimiterIdx + 1);

		return new Street(streetName, streetNumber);
	}

	private static @NonNull ZipCode parseZipCode(@NonNull final String zipCodeAndDistrict) throws AddressParseException {
		final String[] splitDistrictAndZipCode = zipCodeAndDistrict.split(" ", 2);
		if(splitDistrictAndZipCode.length < 2) {
			throw new AddressParseException("Expected zip code and district to be space separated, got: " + zipCodeAndDistrict);
		}

		return new ZipCode(splitDistrictAndZipCode[0], splitDistrictAndZipCode[1]);
	}


	private static Optional<String> parseTown(@NonNull final String[] parts) throws AddressParseException {
		return switch (parts.length) {
			case 2 -> Optional.empty();
			case 3 -> isDesignation(parts[1]) ? Optional.empty() : Optional.of(parts[1]);
			case 4 -> Optional.of(parts[2]);
			default -> throw new AddressParseException("Unexpected parts count: " + parts.length);
		};
	}

	/**
	 * A designation is a complete floor token with an optional door token, e.g. "st.", "2. th." or "1. 101".
	 * Testing for a dot alone would misread supplementary town names such as "St. Andst" or "Gl. Rye".
	 */
	private static boolean isDesignation(@NonNull final String part) {
		return DESIGNATION.matcher(part.trim()).matches();
	}
}
