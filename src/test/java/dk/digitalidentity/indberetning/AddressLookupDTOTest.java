package dk.digitalidentity.indberetning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dk.digitalidentity.indberetning.service.dto.AddressLookupDTO;

public class AddressLookupDTOTest {

	@Nested
	class NormalizeHouseNumber {

		@Test
		void shouldStripFloorAndDoorDesignation() {
			final var result = new AddressLookupDTO("Vestergade", "5, 2. th", "6580");

			assertThat(result.streetNumber()).isEqualTo("5");
		}

		@Test
		void shouldStripDesignationFromHouseNumberWithLetter() {
			final var result = new AddressLookupDTO("Parkvej", "12B, st. tv", "3400");

			assertThat(result.streetNumber()).isEqualTo("12B");
		}

		@Test
		void shouldOnlySplitOnTheFirstComma() {
			final var result = new AddressLookupDTO("Hovedgaden", "7, 3. sal, bagbygningen", "2100");

			assertThat(result.streetNumber()).isEqualTo("7");
		}

		@Test
		void shouldTrimSurroundingWhitespace() {
			final var result = new AddressLookupDTO("Hovedgaden", " 7 , 3. th", "2100");

			assertThat(result.streetNumber()).isEqualTo("7");
		}

		@ParameterizedTest
		@ValueSource(strings = { "5", "12B", "7 A" })
		void shouldLeaveHouseNumbersWithoutDesignationUntouched(final String houseNumber) {
			final var result = new AddressLookupDTO("Vestergade", houseNumber, "6580");

			assertThat(result.streetNumber()).isEqualTo(houseNumber);
		}

		@Test
		void shouldKeepOriginalWhenNothingPrecedesTheComma() {
			final var result = new AddressLookupDTO("Vestergade", ", 3. th", "6580");

			assertThat(result.streetNumber()).isEqualTo(", 3. th");
		}

		@Test
		void shouldTolerateNullHouseNumber() {
			final var result = new AddressLookupDTO("Vestergade", null, "6580");

			assertThat(result.streetNumber()).isNull();
		}

		@Test
		void shouldLeaveOtherFieldsUntouched() {
			final var result = new AddressLookupDTO("Vestergade", "5, 2. th", "6580");

			assertThat(result.streetName()).isEqualTo("Vestergade");
			assertThat(result.postalCode()).isEqualTo("6580");
		}
	}
}
