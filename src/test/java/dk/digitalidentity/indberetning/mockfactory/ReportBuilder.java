package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Fluent builder for Report test fixtures.
 * Defaults to a minimal valid CALCULATED report with sensible values.
 */
public class ReportBuilder {

    private final Report report = new Report();

    public ReportBuilder() {
        report.setStatus(ReportStatus.PENDING);
        report.setCalculationType(CalculationType.CALCULATED);
        report.setDriveDate(LocalDate.of(2024, 11, 15));
        report.setRawDistance(10.0);
        report.setDistance(10.0);
        report.setKmRate(370);
        report.setFromApp(false);
        report.setRoundTrip(false);
        report.setFourKmRule(false);
        report.setStartsAtHome(false);
        report.setEndsAtHome(false);
        report.setExtraDistance(false);
        report.setRecalculate(false);
    }

    public ReportBuilder driveDate(LocalDate date) { report.setDriveDate(date); return this; }
    public ReportBuilder rawDistance(double d) { report.setRawDistance(d); return this; }
    public ReportBuilder distance(double d) { report.setDistance(d); return this; }
    public ReportBuilder kmRate(double rate) { report.setKmRate(rate); return this; }
    public ReportBuilder roundTrip(boolean v) { report.setRoundTrip(v); return this; }
    public ReportBuilder fourKmRule(boolean v) { report.setFourKmRule(v); return this; }
    public ReportBuilder startsAtHome(boolean v) { report.setStartsAtHome(v); return this; }
    public ReportBuilder endsAtHome(boolean v) { report.setEndsAtHome(v); return this; }
    public ReportBuilder fromApp(boolean v) { report.setFromApp(v); return this; }
    public ReportBuilder calculationType(CalculationType t) { report.setCalculationType(t); return this; }
    public ReportBuilder homeToBorderDistance(double d) { report.setHomeToBorderDistance(d); return this; }
    public ReportBuilder status(ReportStatus s) { report.setStatus(s); return this; }
    public ReportBuilder person(Person p) { report.setPerson(p); return this; }
    public ReportBuilder employment(Employment e) { report.setEmployment(e); return this; }

    public ReportBuilder coords(List<GpsCoordinate> coords) {
        report.getCoords().clear();
        report.getCoords().addAll(coords);
        return this;
    }

    public Report build() { return report; }

    public static GpsCoordinate startCoord(double lat, double lon) {
        GpsCoordinate c = new GpsCoordinate();
        c.setLatitude(lat);
        c.setLongitude(lon);
        c.setStartPoint(true);
        c.setEndPoint(false);
        return c;
    }

    public static GpsCoordinate endCoord(double lat, double lon) {
        GpsCoordinate c = new GpsCoordinate();
        c.setLatitude(lat);
        c.setLongitude(lon);
        c.setStartPoint(false);
        c.setEndPoint(true);
        return c;
    }
}
