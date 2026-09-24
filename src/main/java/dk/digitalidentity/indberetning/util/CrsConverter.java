package dk.digitalidentity.indberetning.util;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import dk.digitalidentity.indberetning.model.geometry.CoordinateType;
import dk.digitalidentity.indberetning.model.geometry.Point;

public class CrsConverter {
	private static final CRSFactory factory;
	private static final CoordinateReferenceSystem crs4326;
	private static final CoordinateReferenceSystem crs25832;
	private static final CoordinateTransformFactory ctFactory;

    static {
        factory = new CRSFactory();
        crs4326  = factory.createFromName("EPSG:4326");
        crs25832 = factory.createFromName("EPSG:25832");
        ctFactory = new CoordinateTransformFactory();
    }

    public static Point toUtm(Point point) {
		if(point.type() == CoordinateType.UTM) {
			return point;
		}

        CoordinateTransform transformer = ctFactory.createTransform(crs4326, crs25832);

        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(new ProjCoordinate(point.lon(), point.lat()), result);
        return new Point(result.y, result.x, CoordinateType.UTM);
    }

    public static Point toWgs84(Point point) {
		if(point.type() == CoordinateType.WGS84) {
			return point;
		}

        CoordinateTransform transformer = ctFactory.createTransform(crs25832, crs4326);

        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(new ProjCoordinate(point.lon(), point.lat()), result);
        return new Point(result.y, result.x, CoordinateType.WGS84);
    }
}
