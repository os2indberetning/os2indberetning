package dk.digitalidentity.indberetning.service.dto;

public record AddressLookupDTO(
	String streetName,
	String streetNumber,
	String postalCode
) {
	public AddressLookupDTO {
		streetNumber = normalizeHouseNumber(streetNumber);
	}

	// adressevaelger.dk returns no results for house numbers carrying a floor/door designation ("5, 2. th")
	private static String normalizeHouseNumber(String houseNumber) {
		if (houseNumber == null || !houseNumber.contains(",")) {
			return houseNumber;
		}

		final String candidate = houseNumber.split(",", 2)[0].trim();
		return candidate.isEmpty() ? houseNumber : candidate;
	}
}
