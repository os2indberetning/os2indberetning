package dk.digitalidentity.indberetning.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.maps.model.LatLng;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.entity.AppLogin;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.Route;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.AppLoginService;
import dk.digitalidentity.indberetning.service.AuditLogService;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.GpsCoordinateService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.OnetimePaymentsCalculatorService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.RateService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.RouteService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Hidden
@Slf4j
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class AppUserReportRestController {
    private final AppLoginService appLoginService;
    private final OnetimePaymentsCalculatorService calculatorService;
    private final EmploymentService employmentService;
    private final GpsCoordinateService gpsCoordinateService;
    private final LicensePlateService licensePlateService;
    private final PersonService personService;
    private final RateService rateService;
    private final ReportService reportService;
    private final RouteService routeService;
    private final OS2indberetningConfiguration configuration;
    private final AuditLogService auditLogService;

    public record RequestRec(AuthUuidRequest Authorization, DriveRec DriveReport) {}
    public record AuthUuidRequest(UUID GuId) {}
    public record DriveRec(UUID Uuid, String Purpose, long EmploymentId, long RateId, String ManualEntryRemark, boolean StartsAtHome, boolean EndsAtHome, boolean FourKmRule, String Date, double HomeToBorderDistance, RouteRec route, long ProfileId) {}
    public record GPSCoordinatesRec(double Latitude, double Longitude, boolean IsViaPoint) {}
    public record RouteRec(double TotalDistance, ArrayList<GPSCoordinatesRec> GPSCoordinates) {}

    // If its not transactional, we might end up saving in DB regardless of the outcome
    @Transactional
    @PostMapping("/appapi/report")
    public ResponseEntity<String> report(@RequestBody RequestRec req) throws JsonProcessingException {
        try {
			if (configuration.isLogAllAppReports()) {
                try {
                    auditLogService.saveSystem(LogAction.ACCEPT_REPORT, "Received app report", req);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
			}

            if (!configuration.isAllowNewReports()) {
                return ResponseEntity.badRequest().build();
            }

            Report report = new Report();
            Person person = personService.getById(req.DriveReport.ProfileId);
            AppLogin appLogin = appLoginService.getByUuid(req.Authorization.GuId.toString());
            if (appLogin == null || appLogin.getPerson() != person) {
                log.info("App report was rejected due to unauthorized access Applogin: " + (appLogin != null ? appLogin.getId() : "<null>") + " Person: " + (person != null ? person.getId() : "<null>"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!StringUtils.hasLength(req.DriveReport.Date)) {
                return ResponseEntity.badRequest().build();
            }
            if (StringUtils.hasLength(req.DriveReport.ManualEntryRemark) && req.DriveReport.ManualEntryRemark.length() > 255) {
                log.warn("ManualEntryRemark longer than 255 characters! Person: " + person.getId());
                auditLogService.save(person, LogAction.REJECT_APP_REPORT, "ManualEntryRemark longer than 255 characters", req);
                return ResponseEntity.badRequest().build();
            }

            LocalDate driveDate = parseDate(req.DriveReport.Date).toLocalDate();
            report.setStatus(ReportStatus.PENDING);
            report.setAppUuid(req.DriveReport.Uuid.toString());
            report.setPurpose(req.DriveReport.Purpose);
            report.setEmployment(employmentService.getById(req.DriveReport.EmploymentId));

            Rate rate = rateService.getById(req.DriveReport.RateId);
            if (rate == null) {
                log.warn("No rate was found for reported for rateId:" + req.DriveReport.RateId);
                auditLogService.save(person, LogAction.REJECT_APP_REPORT, "No rate was found for reported for rateId", req);
                return ResponseEntity.badRequest().build();
            }

            report.setKmRate(rate.getRatePerKm());
            report.setKmRateType(rate.getRateType().getName());
            report.setPayType(rate.getRateType().getPayType());
            report.setSequentialNumber(rate.getRateType().getSequentialNumber());
            report.setActiveYear(driveDate.getYear());
            report.setComment(req.DriveReport.ManualEntryRemark);
            report.setStartsAtHome(req.DriveReport.StartsAtHome);
            report.setEndsAtHome(req.DriveReport.EndsAtHome);
            report.setFourKmRule(req.DriveReport.FourKmRule);
            report.setDriveDate(driveDate);
            report.setHomeToBorderDistance(req.DriveReport.HomeToBorderDistance);
            report.setRawDistance(req.DriveReport.route.TotalDistance);
            report.setPerson(person);
            report.setFullName(person.getName());
            report.setEmployment(employmentService.getById(req.DriveReport.EmploymentId));
            report.setEmployeeNumber(report.getEmployment().getEmployeeNumber());
            report.setFromApp(true);
            LicensePlate plate = licensePlateService.getPrimaryPlateByPersonId(report.getPerson());
            if (plate != null) {
                report.setLicensePlate(plate.getRegistrationNumber());
            }
            else {
                report.setLicensePlate("UKENDT");
            }
            report.setCalculationType(CalculationType.CALCULATED);

            // Get address for first and last gps coordinates from app, the app sends about 100 coordinates per kilometer
            LinkedList<GpsCoordinate> gpsCoordinates = new LinkedList<>();
            ArrayList<GPSCoordinatesRec> coordinates = req.DriveReport.route.GPSCoordinates;

            req.DriveReport.route.GPSCoordinates.stream().filter(GPSCoordinatesRec::IsViaPoint).forEach(gpsCoordinatesRec -> log.debug("Via Point:" + getStreetAddress(gpsCoordinatesRec.Latitude, gpsCoordinatesRec.Longitude)));

            GPSCoordinatesRec first = coordinates.get(0);
            coordinates.remove(first);
            GPSCoordinatesRec last = coordinates.get(coordinates.size() - 1);
            coordinates.remove(last);
            // Add first
            gpsCoordinates.add(new GpsCoordinate(
                    first.Latitude,
                    first.Longitude,
                    false,
                    getStreetAddress(first.Latitude, first.Longitude),
                    report,
                    true,
                    false,
                    0
            ));

            // Add via points
            int pointNumber = 1;
			for (GPSCoordinatesRec coordinate : coordinates) {
				if (coordinate.IsViaPoint) {
					gpsCoordinates.add(new GpsCoordinate(
							coordinate.Latitude,
							coordinate.Longitude,
							true,
							getStreetAddress(coordinate.Latitude, coordinate.Longitude),
							report,
							false,
							false,
                            pointNumber
					));
                    pointNumber++;
				}
			}

            // Add last
            gpsCoordinates.add(new GpsCoordinate(
                    last.Latitude,
                    last.Longitude,
                    false,
                    getStreetAddress(last.Latitude, last.Longitude),
                    report,
                    false,
                    true,
                    gpsCoordinates.size()
            ));
            for (GpsCoordinate gpsCoordinate : gpsCoordinates) {
                if (gpsCoordinate.getAddress() == null || gpsCoordinate.getAddress().isEmpty()) {
                    log.warn("The route includes unroutable or invalid points, aborting!");
                    return new ResponseEntity<>("There was a malformed GPS coordinate, aborting...", HttpStatus.BAD_REQUEST);
                }
            }
            reportService.save(report);

            List<Report> reports = reportService.getByPersonAndDriveDate(person, report.getDriveDate());
            gpsCoordinateService.saveAll(gpsCoordinates);
            report.setCoords(gpsCoordinates);

            Route route = new Route();
            List<LatLng> latLngList = coordinates.stream().map(gpsCoordinatesRec -> new LatLng(gpsCoordinatesRec.Latitude, gpsCoordinatesRec.Longitude)).toList();
            route.setRouteGeometry(routeService.encodeLatLngToPolyline(latLngList));
            route.setReport(report);
            routeService.save(route);
            report.setRoute(route);

            List<Report> updatedReports = calculatorService.calculate(reports);
            reportService.saveAll(updatedReports);

            return ResponseEntity.ok().build();
        }
        catch (Exception ex) {
            auditLogService.saveSystem(LogAction.REJECT_APP_REPORT, "App reporting failed", req);
            throw ex;
        }
    }

    private static LocalDateTime parseDate(String date) {
        date = date.replace(" ", "T");
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("yyyy-MM-dd['T'HH:mm:ss][.SSS][.SS]['Z']")
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter();

        return LocalDateTime.parse(date, formatter);
    }

    public String getStreetAddress(double lat, double lng) {
        RouteService.AddressDTO address = routeService.latLngToAddressObj(lat, lng);
        if (address == null) {
            log.warn("The address is null");
            return null;
        }

        StringBuilder b = new StringBuilder();
        b.append(address.road().substring(1,address.road().length()-1)).append(" ").append(address.number().substring(1,address.number().length()-1)).append(", ").append(address.postcode()).append(" ").append(address.city().substring(1,address.city().length()-1));
        return b.toString();
    }
}
