package dk.digitalidentity.indberetning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dk.digitalidentity.indberetning.model.AddressDTO;
import dk.digitalidentity.indberetning.util.AddressUtils;
import dk.digitalidentity.indberetning.util.exception.AddressParseException;

public class AddressDTOUtilsTest {

	@Nested
	class SplitFullAddressDTO {

		@Nested
		class BasicAddresses {

			@Test
			void shouldParseAddressWithOnlyStreetAndZip() throws AddressParseException {
				final var fullAddress = "Hovedgaden 1, 2100 København";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Hovedgaden");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("2100");
				assertThat(result.zipCode().district()).isEqualTo("København");
			}

			@Test
			void shouldParseAddressWithLetterInHouseNumber() throws AddressParseException {
				final var fullAddress = "Parkvej 12A, 3400 Hillerød";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Parkvej");
				assertThat(result.street().number()).isEqualTo("12A");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("3400");
				assertThat(result.zipCode().district()).isEqualTo("Hillerød");
			}

			@Test
			void shouldParseAddressWithSupplementaryTownName() throws AddressParseException {
				final var fullAddress = "Vestergade 5, Nærlund, 4000 Roskilde";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Vestergade");
				assertThat(result.street().number()).isEqualTo("5");
				assertThat(result.town()).contains("Nærlund");
				assertThat(result.zipCode().code()).isEqualTo("4000");
				assertThat(result.zipCode().district()).isEqualTo("Roskilde");
			}
		}

		/**
		 * The only production caller is GeoAddressDTO, fed by DAR's adgangsadressebetegnelse.
		 * An access address never carries a floor/door designation, so a 3-part address is
		 * always street, supplementary town, zip - and Danish town names frequently contain dots.
		 */
		@Nested
		class SupplementaryTownNamesWithDots {

			@Test
			void shouldKeepTownAbbreviatedWithStore() throws AddressParseException {
				final var fullAddress = "Gamstvej 12, St. Andst, 6600 Vejen";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Gamstvej");
				assertThat(result.street().number()).isEqualTo("12");
				assertThat(result.town()).contains("St. Andst");
				assertThat(result.zipCode().code()).isEqualTo("6600");
				assertThat(result.zipCode().district()).isEqualTo("Vejen");
			}

			@Test
			void shouldKeepTownAbbreviatedWithGammel() throws AddressParseException {
				final var fullAddress = "Ryesgade 4, Gl. Rye, 8680 Ry";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.town()).contains("Gl. Rye");
				assertThat(result.zipCode().district()).isEqualTo("Ry");
			}

			@Test
			void shouldKeepTownAbbreviatedWithSoender() throws AddressParseException {
				final var fullAddress = "Bygaden 7, Sdr. Bjert, 6091 Bjert";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.town()).contains("Sdr. Bjert");
			}

			@Test
			void shouldKeepTownAbbreviatedWithNoerre() throws AddressParseException {
				final var fullAddress = "Hovedgaden 30, Nr. Snede, 8766 Nørre Snede";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.town()).contains("Nr. Snede");
				assertThat(result.zipCode().district()).isEqualTo("Nørre Snede");
			}

			@Test
			void shouldKeepDottedTownWhenDesignationIsAlsoPresent() throws AddressParseException {
				final var fullAddress = "Langgade 42B, 2. th., Gl. Rye, 8680 Ry";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().number()).isEqualTo("42B");
				assertThat(result.town()).contains("Gl. Rye");
			}
		}

		@Nested
		class FloorDesignations {

			@Test
			void shouldParseAddressWithSt() throws AddressParseException {
				final var fullAddress = "Hovedgaden 1, st., 2100 København";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Hovedgaden");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("2100");
				assertThat(result.zipCode().district()).isEqualTo("København");
			}

			@Test
			void shouldParseAddressWithKl() throws AddressParseException {
				final var fullAddress = "Boulevarden 22, kl., 8000 Aarhus C";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Boulevarden");
				assertThat(result.street().number()).isEqualTo("22");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("8000");
				assertThat(result.zipCode().district()).isEqualTo("Aarhus C");
			}

			@Test
			void shouldParseAddressWithFloorNumber() throws AddressParseException {
				String fullAddress = "Langebro 3, 1., 1300 København K";

				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Langebro");
				assertThat(result.street().number()).isEqualTo("3");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1300");
				assertThat(result.zipCode().district()).isEqualTo("København K");
			}
		}

		@Nested
		class DoorDesignations {

			@Test
			void shouldParseAddressWithTv() throws AddressParseException {
				final var fullAddress = "Gade 1, 1. tv., 1000 København C";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Gade");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1000");
				assertThat(result.zipCode().district()).isEqualTo("København C");
			}

			@Test
			void shouldParseAddressWithTh() throws AddressParseException {
				final var fullAddress = "Gade 1, 2. th., 1000 København C";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Gade");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1000");
				assertThat(result.zipCode().district()).isEqualTo("København C");
			}

			@Test
			void shouldParseAddressWithMf() throws AddressParseException {
				final var fullAddress = "Gade 1, 3.  mf., 1000 København C";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Gade");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1000");
				assertThat(result.zipCode().district()).isEqualTo("København C");
			}

			@Test
			void shouldParseAddressWithDoorNumber() throws AddressParseException {
				final var fullAddress = "Gade 1, 1.  101, 1000 København C";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Gade");
				assertThat(result.street().number()).isEqualTo("1");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1000");
				assertThat(result.zipCode().district()).isEqualTo("København C");
			}
		}

		@Nested
		class CombinedAddresses {

			@Test
			void shouldParseAddressWithFloorAndDoor() throws AddressParseException {
				final var fullAddress = "Strøget 10, 1. tv., 1000 København K";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Strøget");
				assertThat(result.street().number()).isEqualTo("10");
				assertThat(result.town()).isEmpty();
				assertThat(result.zipCode().code()).isEqualTo("1000");
				assertThat(result.zipCode().district()).isEqualTo("København K");
			}

			@Test
			void shouldParseFullAddressWithAllComponents() throws AddressParseException {
				final var fullAddress = "Langgade 42B, 2. th., Nærum, 2850 Nærum";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Langgade");
				assertThat(result.street().number()).isEqualTo("42B");
				assertThat(result.town()).contains("Nærum");
				assertThat(result.zipCode().code()).isEqualTo("2850");
				assertThat(result.zipCode().district()).isEqualTo("Nærum");
			}
		}

		@Nested
		class EdgeCases {

			@ParameterizedTest
			@ValueSource(strings = {
				"A 1, 0000 B",
				"X 99, 9999 Z"
			})
			void shouldHandleShortAddressComponents(String fullAddress) throws AddressParseException {
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isNotEmpty();
				assertThat(result.street().number()).isNotEmpty();
				assertThat(result.zipCode().code()).isNotEmpty();
				assertThat(result.zipCode().district()).isNotEmpty();
			}

			@Test
			void shouldHandleStreetNameWithDots() throws AddressParseException {
				final var fullAddress = "Nr. Bjertvej 87A, 6000 Kolding";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result.street().name()).isEqualTo("Nr. Bjertvej");
				assertThat(result.street().number()).isEqualTo("87A");
				assertThat(result.zipCode().code()).isEqualTo("6000");
				assertThat(result.zipCode().district()).isEqualTo("Kolding");
			}

			@Test
			void shouldReturnAddressRecord() throws AddressParseException {
				final var fullAddress = "Vestergade 10, 8800 Viborg";
				final var result = AddressUtils.parseAddress(fullAddress);

				assertThat(result).isInstanceOf(AddressDTO.class);
				assertThat(result.street().name()).isNotEmpty();
				assertThat(result.street().number()).isNotEmpty();
				assertThat(result.zipCode()).isNotNull();
				assertThat(result.zipCode().code()).isNotEmpty();
				assertThat(result.zipCode().district()).isNotEmpty();
			}
		}

		@Nested
		class ErrorCases {

			@ParameterizedTest
			@ValueSource(strings = {
				"Novector, 1234 City",
				"12345, 1234 City"
			})
			void shouldThrowWhenNoSpaceBetweenStreetNameAndNumber(String fullAddress) {
				assertThatThrownBy(() -> AddressUtils.parseAddress(fullAddress))
					.isInstanceOf(AddressParseException.class);
			}

			@Test
			void shouldThrowOnEmptyString() {
				assertThatThrownBy(() -> AddressUtils.parseAddress(""))
					.isInstanceOf(AddressParseException.class);
			}

			@Test
			void shouldThrowOnMissingComma() {
				final var fullAddress = "Hovedgaden 1 2100 København";
				assertThatThrownBy(() -> AddressUtils.parseAddress(fullAddress))
					.isInstanceOf(AddressParseException.class);
			}

			@Test
			void shouldThrowOnMissingSpaceInZipCode() {
				assertThatThrownBy(() -> AddressUtils.parseAddress("Hovedgaden 1, 2100København"))
					.isInstanceOf(AddressParseException.class);
			}
		}
	}
}
