package dk.digitalidentity.indberetning.controller.mvc;

import com.google.maps.model.EncodedPolyline;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.model.entity.Route;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.PersonalRouteService;
import dk.digitalidentity.indberetning.service.RateService;
import dk.digitalidentity.indberetning.service.RateTypeService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.RouteService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class ReportController {
    private final AddressService addressService;
    private final EmploymentService employmentService;
    private final LicensePlateService licensePlateService;
    private final RateService rateService;
    private final SecurityUtil securityUtil;
    private final ReportService reportService;
    private final OrgUnitService orgUnitService;
    private final RateTypeService rateTypeService;
    private final RouteService routeService;
    private final PersonalRouteService personalRouteService;
    private final OS2indberetningConfiguration configuration;

    record RequestReportDTO(long personId, long orgUnitId, LocalDate from, LocalDate to) {}

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("dto", null);
        model.addAttribute("allowTimePicker", configuration.isAllowTimePickerAndTimeEstimation());
        return "report/report";
    }

    record InitialReportDTO(long id) {}
    @GetMapping("/report/{id}")
    public String report(@PathVariable long id, Model model) {
        model.addAttribute("dto", new InitialReportDTO(id));
        model.addAttribute("allowTimePicker", configuration.isAllowTimePickerAndTimeEstimation());
        return "report/report";
    }


    @GetMapping("/report/reportFragment")
    public String reportFragment(Model model) {
        Person currentUser = securityUtil.getPerson();

        final List<Employment> employments = employmentService.getByPersonButNotAfterCloseDelay(currentUser);
        employments.sort(Comparator.comparing(Employment::getPosition, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("employments", employments);


        List<LicensePlate> licensePlates = licensePlateService.getByPersonId(currentUser.getId());
        licensePlates.sort(Comparator.comparing(LicensePlate::getRegistrationNumber, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("plates", licensePlates);

        List<Rate> activeRates = rateService.getByActiveYear(Year.now().getValue());
        activeRates.sort(Comparator.comparing(Rate::getRatePerKm, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("rates", activeRates);

		if (activeRates != null && !employments.isEmpty()) {
            Optional<Rate> primeActiveRate = rateService. getPreSelectedRate(activeRates, currentUser, employments.stream().findFirst().orElse(null));
            model.addAttribute("activeRate", primeActiveRate.orElse(null));
		}

        model.addAttribute("aAddresses", addressService.getAvailableAddressesByDate(currentUser, LocalDateTime.now()));
        model.addAttribute("primaryAddress", addressService.getPrimary());

        List<PersonalRoute> personalRoutes = personalRouteService.findByPerson(currentUser);
        personalRoutes.sort(Comparator.comparing(PersonalRoute::getDescription, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("personalRoutes", personalRoutes);
        model.addAttribute("allowTimePicker", configuration.isAllowTimePickerAndTimeEstimation());

        return "report/reportFragment";
    }

    @Builder
    record ReportDTO(long id, LocalDate driveDate, String purpose, String licensePlate, Long employmentId, dk.digitalidentity.indberetning.model.entity.enums.CalculationType calculationType, boolean fourKmRule, double homeToBorderDistance, boolean roundTrip, double rawDistance, Rate rate, String comment, boolean startsAtHome, boolean endsAtHome, String routeGeometry, List<GpsCoordinate> coords, GpsCoordinate first, GpsCoordinate last) {}
    @GetMapping("/report/reportFragment/{id}")
    public String reportFragment(@PathVariable long id, Model model) {
        Person currentUser = securityUtil.getPerson();
        Report toBeEdited = reportService.getById(id);
        if (toBeEdited == null || currentUser == null || !Objects.equals(currentUser.getId(), toBeEdited.getPerson().getId())) {
            return "error";
        }

        // Sort in natural order
        List<LicensePlate> licensePlates = licensePlateService.getByPersonId(currentUser.getId());
        licensePlates.sort(Comparator.comparing(LicensePlate::getRegistrationNumber, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("plates", licensePlates);

        List<Employment> employments = employmentService.getByPerson(currentUser);
        employments.sort(Comparator.comparing(Employment::getPosition, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("employments", employments);

        List<Rate> byActiveYear = rateService.getByActiveYear(toBeEdited.getDriveDate().getYear());
        byActiveYear.sort(Comparator.comparing(Rate::getRatePerKm, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("rates", byActiveYear);

        model.addAttribute("aAddresses", addressService.getAvailableAddressesByDate(currentUser, toBeEdited.getDriveDate().atStartOfDay()));
        model.addAttribute("primaryAddress", addressService.getPrimary());

		ReportDTO.ReportDTOBuilder builder = ReportDTO.builder();
		builder.id(toBeEdited.getId());
		builder.driveDate(toBeEdited.getDriveDate());
		builder.purpose(toBeEdited.getPurpose());
		builder.licensePlate(toBeEdited.getLicensePlate());
		builder.employmentId(toBeEdited.getEmployment().getId());
		builder.calculationType(toBeEdited.getCalculationType());
		builder.fourKmRule(toBeEdited.isFourKmRule());
		builder.homeToBorderDistance(toBeEdited.getHomeToBorderDistance());
		builder.roundTrip(toBeEdited.isRoundTrip());
		builder.rawDistance(toBeEdited.getRawDistance());

        // Used for CalculationType.READ
        builder.comment(toBeEdited.getComment());
        builder.startsAtHome(toBeEdited.isStartsAtHome());
        builder.endsAtHome(toBeEdited.isEndsAtHome());

        // Fallback to name for reports created before V1_51
        RateType rateType = toBeEdited.getKmRateTypeId() != null
                ? rateTypeService.getById(toBeEdited.getKmRateTypeId())
                : rateTypeService.getByName(toBeEdited.getKmRateType());
        if (rateType != null) {
            builder.rate(rateService.getByYearAndType(toBeEdited.getDriveDate().getYear(), rateType));
        }

        Route route = toBeEdited.getRoute();
        if (route != null) {
            EncodedPolyline encPoly = new EncodedPolyline(toBeEdited.getRoute().getRouteGeometry());
            String decodedRoute = routeService.convertGoogleLatLngToGeoJsonRoute(encPoly.decodePath());
            builder.routeGeometry(decodedRoute);
        }

        List<GpsCoordinate> coords = toBeEdited.getCoords();
        if (coords != null && !coords.isEmpty()) {
            coords.sort(Comparator.comparingInt(GpsCoordinate::getPointNumber));
            GpsCoordinate first = coords.stream().filter(GpsCoordinate::isStartPoint).findFirst().orElse(null);

            builder.first(first);
            GpsCoordinate last = toBeEdited.isRoundTrip() ? coords.stream().max(Comparator.comparingInt(GpsCoordinate::getPointNumber)).orElse(null) : coords.stream().filter(GpsCoordinate::isEndPoint).findFirst().orElse(null);
            builder.last(last);

            coords.remove(first);
            coords.remove(last);

            builder.coords(coords);
        }

        ReportDTO dto = builder.build();

        model.addAttribute("dto", dto);

        List<PersonalRoute> personalRoutes = personalRouteService.findByPerson(currentUser);
        personalRoutes.sort(Comparator.comparing(PersonalRoute::getDescription, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("personalRoutes", personalRoutes);
        model.addAttribute("allowTimePicker", configuration.isAllowTimePickerAndTimeEstimation());

        return "report/reportFragment";
    }

    @GetMapping("/report/list")
    public String listView(Model model) {
        model.addAttribute("personal", true);
        return "report/list";
    }
    

    @GetMapping("/personal/report/list")
    public String reportFragment(Model model, @RequestParam(name = "status") ReportStatus status, @RequestParam(name = "page", defaultValue = "0") int pageNr) {
        reportService.populatePersonalReportList(model, status);
        model.addAttribute("personal", true);
        return "report/listFragment :: report-list";
    }

    @GetMapping("/reportCard")
    public String reportCard(Model model) {
        List<OrgUnit> orgUnits = orgUnitService.getAll();
        orgUnits.sort(Comparator.comparing(OrgUnit::getLongDescription, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.addAttribute("orgUnits", orgUnits);
        return "reportCard/reportCardFragment";
    }

}
