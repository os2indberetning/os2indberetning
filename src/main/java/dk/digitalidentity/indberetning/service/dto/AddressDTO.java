package dk.digitalidentity.indberetning.service.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Setter
public class AddressDTO {
	private String street;

	@Getter
	private int postalCode;

	private String city;

	public AddressDTO(String street, int postalCode, String city) {
		this.street = street;
		this.postalCode = postalCode;
		this.city = city;
	}

	public String getCity() {
		return StringUtils.hasLength(city) ? city: "";
	}

	public String getStreetName() {
		if (!StringUtils.hasLength(street)) {
			return "";
		}

		String[] streetName = street.split("(\\d)+", 2);
		String val = streetName[0].trim();

		return StringUtils.hasLength(val) ? val : "";
	}

	public String getStreetNumber() {
		if (!StringUtils.hasLength(street)) {
			return "";
		}

		String[] number = street.split("(\\D)+", 2);
		String val = number[number.length - 1].trim();

		return StringUtils.hasLength(val) ? val : "";
	}

	@Override
	public String toString() {
		return "AddressDTO[" + "street=" + street + ", " + "postalCode=" + postalCode + ", " + "city=" + city + ']';
	}
}