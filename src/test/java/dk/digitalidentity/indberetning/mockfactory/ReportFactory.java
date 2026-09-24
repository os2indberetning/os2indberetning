package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;

import java.time.LocalDate;

public class ReportFactory {

    public static Report accepted(long id, String employeeNumber, LocalDate driveDate, double distance) {
        Report r = new Report();
        r.setId(id);
        r.setEmployeeNumber(employeeNumber);
        r.setDriveDate(driveDate);
        r.setDistance(distance);
        r.setStatus(ReportStatus.ACCEPTED);
        r.setPayType(1);
        r.setSequentialNumber(1);
        return r;
    }
}
