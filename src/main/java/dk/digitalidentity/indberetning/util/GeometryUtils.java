package dk.digitalidentity.indberetning.util;

import org.jspecify.annotations.NonNull;

import dk.digitalidentity.indberetning.model.geometry.Point;

public class GeometryUtils {

	/**
	 * Calculates the distance between 2 points using Euclidean distance
	 */
	public static double distanceBetweenPoints(@NonNull Point a, @NonNull Point b) {
		if(a.type() != b.type()) {
			throw new IllegalArgumentException("Points are not same coordinate type");
		}

		return Math.sqrt(
				Math.pow(a.lon() - b.lon(), 2) +
				Math.pow(a.lat() - b.lat(), 2)
		);
	}
}
