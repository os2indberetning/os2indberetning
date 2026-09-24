package dk.digitalidentity.indberetning.util;

import org.jspecify.annotations.NonNull;

import dk.digitalidentity.indberetning.model.geometry.CoordinateType;
import dk.digitalidentity.indberetning.model.geometry.Point;

public class WKTParser {

	public static @NonNull Point parsePoint(@NonNull String wkt, CoordinateType type) {
		if(wkt.isBlank()) {
			throw new IllegalArgumentException("WKT point was blank");
		}

		final int start = wkt.indexOf('(');
		final int end = wkt.indexOf(')');
		if (start == -1 || end == -1 || start >= end) {
			throw new IllegalArgumentException("Invalid WKT point: " + wkt);
		}

		final String[] parts = wkt.substring(start + 1, end).trim().split("\\s+");
		if (parts.length < 2) {
			throw new IllegalArgumentException("Invalid WKT point: " + wkt);
		}

		try {
			final double x = Double.parseDouble(parts[0]);
			final double y = Double.parseDouble(parts[1]);
			return new Point(y, x, type);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid coordinates in WKT point: " + wkt, e);
		}
	}
}
