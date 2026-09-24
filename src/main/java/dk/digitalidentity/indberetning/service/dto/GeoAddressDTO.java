package dk.digitalidentity.indberetning.service.dto;

import org.jspecify.annotations.NonNull;

import dk.digitalidentity.indberetning.model.AddressDTO;
import dk.digitalidentity.indberetning.model.geometry.Point;
import dk.digitalidentity.indberetning.util.AddressUtils;
import dk.digitalidentity.indberetning.util.CrsConverter;
import dk.digitalidentity.indberetning.util.exception.AddressParseException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GeoAddressDTO {
	public record Vejstykke(String navn) {}
	public record Postnummer(String nr, String navn) {}

	Vejstykke vejstykke;
	String husnr;
	Postnummer postnummer;

	private double lat;
	private double lon;

	public GeoAddressDTO(@NonNull final String fullAddress, @NonNull final Point point) throws AddressParseException {
		final Point pointWGS = CrsConverter.toWgs84(point);
		final AddressDTO addressDTO = AddressUtils.parseAddress(fullAddress);

		this.vejstykke = new Vejstykke(addressDTO.street().name());
		this.husnr = addressDTO.street().number();
		this.postnummer = new Postnummer(addressDTO.zipCode().code(), addressDTO.zipCode().district());
		this.lat = pointWGS.lat();
		this.lon = pointWGS.lon();
	}
}
