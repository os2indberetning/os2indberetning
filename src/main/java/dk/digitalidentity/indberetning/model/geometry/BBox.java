package dk.digitalidentity.indberetning.model.geometry;

import org.jspecify.annotations.NonNull;

public record BBox(
	@NonNull Point bottomLeft,
	@NonNull Point bottomRight,
	@NonNull Point topLeft,
	@NonNull Point topRight
) {

	/**
	 * Builds a square box centered on the given point.
	 *
	 * @param point centre of the box. Must be in a projected coordinate system such as UTM, since the
	 *              same offset is applied to both axes. A WGS-84 point yields a skewed box, because a
	 *              degree of longitude is shorter than a degree of latitude at Danish latitudes.
	 * @param size  half-width of the box in the unit of the coordinate system, so metres for UTM.
	 *              The resulting box measures 2 * size on each side.
	 */
	public BBox(@NonNull final Point point, final double size) {
		this(new Point(point.lat() - size, point.lon() - size, point.type()),
			 new Point(point.lat() - size, point.lon() + size, point.type()),
			 new Point(point.lat() + size, point.lon() - size, point.type()),
			 new Point(point.lat() + size, point.lon() + size, point.type()));
	}

	public String toWKTPolygon() {
		return String.format("POLYGON ((%s, %s, %s, %s, %s))", bottomLeft.toWKT(), topLeft.toWKT(), topRight.toWKT(), bottomRight.toWKT(), bottomLeft.toWKT());
	}
}
