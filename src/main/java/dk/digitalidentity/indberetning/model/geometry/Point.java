package dk.digitalidentity.indberetning.model.geometry;

public record Point(double lat, double lon, CoordinateType type) {
	public String toWKT() {
		return String.format("%s %s", lon, lat);
	}
}
