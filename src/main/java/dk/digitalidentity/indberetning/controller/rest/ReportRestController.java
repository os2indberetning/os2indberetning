package dk.digitalidentity.indberetning.controller.rest;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import io.swagger.v3.oas.annotations.Hidden;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dk.digitalidentity.indberetning.model.datatable.dao.ReportDatatableDao;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.ReportView;
import dk.digitalidentity.indberetning.model.entity.Route;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.OverridePaymentType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.RequireAdministratorOrApprover;
import dk.digitalidentity.indberetning.security.RequireApprover;
import dk.digitalidentity.indberetning.security.Roles;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.DatatableSpecBuilderUtil;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.GpsCoordinateService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.OnetimePaymentsCalculatorService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.RateService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.RouteService;
import dk.digitalidentity.indberetning.service.SearchUtil;
import dk.digitalidentity.indberetning.service.SixtyDayRuleService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Hidden
@Slf4j
@NoRoleRequired
@RestController
@RequiredArgsConstructor
public class ReportRestController {

    private final EmploymentService employmentService;
    private final GpsCoordinateService gpsCoordinateService;
    private final LicensePlateService licensePlateService;
    private final OnetimePaymentsCalculatorService calculatorService;
    private final RateService rateService;
    private final ReportService reportService;
    private final RouteService routeService;
    private final SecurityUtil securityUtil;
    private final PersonService personService;
    private final OrgUnitService orgUnitService;
    private final SixtyDayRuleService sixtyDayRuleService;
    private final SubstituteService substituteService;
    private final ReportDatatableDao reportDatatableDao;
    private final AddressService addressService;

    public record AddressRecord(String road, String house_number, int postcode, String town, double lat, double lng) {}
    public record ReportRecord(long id, String driveDate, long employmentId, long rateTypeId, long licensePlateId, String purpose, String routeGeometry, boolean fourKmRule, boolean roundTrip, boolean startsAtHome, boolean endsAtHome, CalculationType calculationType, String rawDistance, String comment, double homeToBorder, List<AddressRecord> addressList) {}
    public record ApproveRecord(long reportId, OverridePaymentType type, String value) {}

    @Transactional(rollbackOn = Exception.class)
    @PostMapping("/rest/report/create")
    public ResponseEntity<String> createReport(@RequestBody ReportRecord newRep) {
        // Sanity checks:
        if (!StringUtils.hasLength(newRep.purpose)) {
            return ResponseEntity.badRequest().build();
        }
        Rate rate = rateService.getById(newRep.rateTypeId);
        if (rate == null) {
            return ResponseEntity.badRequest().build();
        }

        Employment employment = employmentService.getById(newRep.employmentId);
        if (employment == null) {
            return ResponseEntity.badRequest().build();
        }

        Report newReport = null;
        if (newRep.id != 0L) {
            newReport = reportService.getById(newRep.id);
            if (ReportStatus.INVOICED.equals(newReport.getStatus()) || ReportStatus.ACCEPTED.equals(newReport.getStatus())) {
                log.warn("Tried to edit report that was already accepted/invoiced!");
                return ResponseEntity.badRequest().build();
            }

            newReport.setApprovedBy(null);
            newReport.setClosedDate(null);
        }
        else {
			newReport = new Report();
		}

        LocalDate driveDate = LocalDate.parse(newRep.driveDate);
        if (LocalDate.now().isBefore(driveDate)) {
            // Reporting future routes is not allowed
            return ResponseEntity.badRequest().build();
        }

        newReport.setCreatedDate(LocalDateTime.now());
        newReport.setStatus(ReportStatus.PENDING);
        newReport.setDriveDate(driveDate);

        Person person = securityUtil.getPerson();
        newReport.setPerson(person);
        newReport.setFullName(person.getName());
        newReport.setKmRate(rate.getRatePerKm());
        newReport.setKmRateType(rate.getRateType().getName());
        newReport.setPayType(rate.getRateType().getPayType());
        newReport.setSequentialNumber(rate.getRateType().getSequentialNumber());
        newReport.setActiveYear(rate.getActiveYear());
        newReport.setEmployment(employment);
        newReport.setEmployeeNumber(employment.getEmployeeNumber());
        newReport.setPurpose(newRep.purpose);
        newReport.setLicensePlate(licensePlateService.getById(newRep.licensePlateId).getRegistrationNumber());
        newReport.setFromApp(false);
        newReport.setFourKmRule(newRep.fourKmRule);
        newReport.setHomeToBorderDistance(newReport.isFourKmRule() ? newRep.homeToBorder : 0);
        newReport.setRoundTrip(newRep.roundTrip);
        newReport.setCalculationType(newRep.calculationType);

        if (newRep.calculationType.equals(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)) {
            newReport.setExtraDistance(false);
        }
        newReport.setRawDistance(Double.parseDouble(newRep.rawDistance));

        if (!CalculationType.READ.equals(newReport.getCalculationType())) {
            Route route = new Route();
            route.setRouteGeometry(routeService.routeEncoder(newRep.routeGeometry));
            routeService.save(route);
            newReport.setRoute(route);
        } else {
            newReport.setStartsAtHome(newRep.startsAtHome);
            newReport.setEndsAtHome(newRep.endsAtHome);
            newReport.setComment(newRep.comment);
        }

        String allApproversString = null;
        List<Person> allApprovers = new ArrayList<>(reportService.findApprovers(newReport));
        if (!allApprovers.isEmpty()) {
            StringBuilder sb = new StringBuilder(allApprovers.removeFirst().getName());
            for (Person approver : allApprovers) {
                sb.append(", ").append(approver.getName());
            }
            allApproversString = sb.toString();
        }
        newReport.setPotentialApprovers(allApproversString);

        reportService.save(newReport);
        List<Report> reports = reportService.getByPersonAndDriveDate(person, newReport.getDriveDate());

        List<GpsCoordinate> coords = new ArrayList<>();
        List<GpsCoordinate> toBeDeleted = new ArrayList<>();
        if (newRep.id != 0L) {
            toBeDeleted = newReport.getCoords();
        }
        if (newRep.addressList() != null && !newRep.addressList().isEmpty()) {
            for (int i = 0; i < newRep.addressList.size(); i++) {
                AddressRecord addr = newRep.addressList.get(i);
                GpsCoordinate gps = new GpsCoordinate();
                gps.setLatitude(addr.lat);
                gps.setLongitude(addr.lng);
                gps.setReport(newReport);
                gps.setAddress(addr.road + " " + addr.house_number + ", " + addr.postcode + " " + addr.town);
                gps.setWaypoint(true);
                gps.setPointNumber(i);
                coords.add(gps);
            }
            GpsCoordinate first = coords.getFirst();
            first.setWaypoint(false);
            first.setStartPoint(true);

            if (newReport.isRoundTrip()) {
                first.setEndPoint(true);
            }
            else {
                GpsCoordinate last = coords.getLast();
                last.setWaypoint(false);
                last.setEndPoint(true);
            }

            newReport.setCoords(coords);
        }

        gpsCoordinateService.deleteAll(toBeDeleted);
        gpsCoordinateService.saveAll(coords);

        // All Reports from a day can have an influence on calculations for each other,
        // so we need to recalculate all reports on a given DriveDate
        List<Report> updatedReports = calculatorService.calculate(reports);
        reportService.saveAll(updatedReports);

        if (!coords.isEmpty()) {
            sixtyDayRuleService.processIndividualSixtyDayRule(newReport, person, coords, driveDate);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/report/delete")
    public ResponseEntity<String> deleteReport(@RequestBody long id) {
        Report toBeDeleted = reportService.getById(id);
        if (toBeDeleted == null) {
            return ResponseEntity.badRequest().build();
        }

        // You can only delete your own reports unless if you're an admin
        Person loggedinPerson = securityUtil.getPerson();
        if (!securityUtil.isAdmin() && !Objects.equals(toBeDeleted.getPerson().getId(), loggedinPerson.getId())) {
            return ResponseEntity.badRequest().build();
        }

        switch (toBeDeleted.getStatus()) {
            case PENDING, ACCEPTED, REJECTED -> {
                ;
			}
            case INVOICED, API_READY, API_FETCHED -> {
                return ResponseEntity.badRequest().build();
			}
			default -> throw new IllegalStateException("Unexpected value: " + toBeDeleted.getStatus());
		}

        Person person = toBeDeleted.getPerson();
        if (person == null) {
            return ResponseEntity.badRequest().build();
        }


        gpsCoordinateService.deleteAll(toBeDeleted.getCoords());
        reportService.delete(id);
        List<Report> reports = reportService.getByPersonAndDriveDate(person, toBeDeleted.getDriveDate());

        if (!reports.isEmpty()) {
            // All Reports from a day can have an influence on calculations for each other,
            // so we need to recalculate all reports on a given DriveDate
            List<Report> updatedReports = calculatorService.calculate(reports);
            reportService.saveAll(updatedReports);
        }

        return ResponseEntity.ok().build();
    }

    @RequireAdministrator
    @PostMapping("/rest/report/rejectInvoiced/{reportId}")
    public ResponseEntity<String>  rejectInvoicedReport(@PathVariable long reportId, @RequestBody String reason) {
        Report report = reportService.getById(reportId);
        if (report == null) {
            return ResponseEntity.badRequest().build();
        }
        else {
            log.info(reason);
            report.setUserComment(reason);
            report.setApprovedBy(securityUtil.getPerson());
            report.setStatus(ReportStatus.REJECTED_AFTER_INVOICE);
            reportService.save(report);
        }
        return ResponseEntity.ok().build();
    }


    // TODO: Finish the update function
    @PostMapping("/rest/report/update")
    public ResponseEntity<String> updateReport(@RequestBody ReportRecord repRec) {
        return ResponseEntity.ok().build();
    }

    @RequireAdministratorOrApprover
    @PostMapping("/rest/report/reject")
    public ResponseEntity<String> rejectReport(@RequestBody ApproveRecord rejectRecord) {
        Report report = reportService.getById(rejectRecord.reportId);
        if (report == null) {
            return ResponseEntity.badRequest().build();
        }

        Person approver = securityUtil.getPerson();

        report.setApprovedBy(approver);
        report.setClosedDate(LocalDateTime.now());
        report.setStatus(ReportStatus.REJECTED);
        report.setUserComment(rejectRecord.value);

        reportService.save(report);

        return ResponseEntity.ok().build();
    }

    @RequireAdministratorOrApprover
    @PostMapping("/rest/report/approve")
    public ResponseEntity<String> approveReport(@RequestBody ApproveRecord approveRecord) {
        Report report = reportService.getById(approveRecord.reportId);
        if (report == null) {
            return ResponseEntity.badRequest().build();
        }

        Person approver = securityUtil.getPerson();

        report.setApprovedBy(approver);
        report.setClosedDate(LocalDateTime.now());
        report.setStatus(ReportStatus.ACCEPTED);

        switch (approveRecord.type) {
            case COST_CENTER:
                report.setOverrideCostCenter(approveRecord.value);
                report.setOverridePspElement(null);
                break;
            case PSP_ELEMENT:
                report.setOverridePspElement(approveRecord.value);
                report.setOverrideCostCenter(null);
                break;
            default:
                report.setOverrideCostCenter(null);
                report.setOverridePspElement(null);
                break;
        }

        reportService.save(report);

        return ResponseEntity.ok().build();
    }

    @RequireAdministratorOrApprover
    @PostMapping("/rest/report/approveMultiple")
    public ResponseEntity<String> approveMultipleReports(@RequestBody List<ApproveRecord> approveRecords) {
        List<Report> reports = new ArrayList<>();
        approveRecords.forEach(approveRecord -> reports.add(reportService.getById(approveRecord.reportId)));
        if(reports.contains(null)) {
            return ResponseEntity.badRequest().build();
        }

        Person approver = securityUtil.getPerson();
        for(Report report: reports) {
            report.setApprovedBy(approver);
            report.setClosedDate(LocalDateTime.now());
            report.setStatus(ReportStatus.ACCEPTED);
            ApproveRecord relevantApproveRecord = approveRecords.get(reports.indexOf(report));

            switch (relevantApproveRecord.type) {
                case COST_CENTER:
                    report.setOverrideCostCenter(relevantApproveRecord.value);
                    report.setOverridePspElement(null);
                    break;
                case PSP_ELEMENT:
                    report.setOverridePspElement(relevantApproveRecord.value);
                    report.setOverrideCostCenter(null);
                    break;
                default:
                    report.setOverrideCostCenter(null);
                    report.setOverridePspElement(null);
                    break;
            }
        }

        reportService.saveAll(reports);
        return ResponseEntity.ok().build();
    }

    record RequestReportDTO(long personId, long orgUnitId, LocalDate from, LocalDate to) {}
    record ResponseDTO(String fullName, String orgName, LocalDate from, LocalDate to, LocalDate now, Set<String> licensePlates, List<ResponseReportDTO> processedReports) {}
    @Builder
    record ResponseReportDTO(LocalDate driveDate, LocalDate createdDate, String personName, String employeeId, String orgName, String purpose, String route, String roundTrip, String extraDistance, String fourKmRule, double homeToBorderDistance, String sixtyDayRule, double distance, double amount, String kmRateType, String kmRate, String status, String approvedBy, LocalDateTime timeOfApproval, boolean flagged60days) {}
    @ModelAttribute
    @RequestMapping("rest/reportCard/getReports")
    public ResponseEntity<?> getReports(@RequestBody RequestReportDTO requestReportDTO) {
        List<Employment> employments = employmentService.getByPersonAndOrgUnit(personService.getById(requestReportDTO.personId), orgUnitService.findById(requestReportDTO.orgUnitId));
        if (employments == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Report> reports = new ArrayList<>();
        for (Employment employment : employments) {
            reports.addAll(reportService.getByPersonAndOrgUnitAndDriveDate(personService.getById(requestReportDTO.personId), employment.getEmployeeNumber(), requestReportDTO.from, requestReportDTO.to));
        }

        Set<String> licensePlates = new HashSet<>();
        List<ResponseReportDTO> reponseReportDTOs = new ArrayList<>();

        for (Report report : reports) {
            ResponseReportDTO reportDTO = ResponseReportDTO.builder()
                    .driveDate(report.getDriveDate())
                    .createdDate(report.getCreatedDate().toLocalDate())
                    .personName(report.getPerson().getName())
                    .employeeId(report.getEmployment().getEmployeeNumber())
                    .orgName(report.getEmployment().getOrgUnit().getLongDescription())
                    .purpose(report.getPurpose())
                    .route(report.getAddressesString())
                    .roundTrip(report.isRoundTrip() ? "Ja" : "Nej")
                    .extraDistance(report.isExtraDistance() ? "Ja" : "Nej")
                    .fourKmRule(report.isFourKmRule() ? "Ja" : "Nej")
                    //                    . ("4-km fratrukket")
                    .homeToBorderDistance(report.getHomeToBorderDistance())
                    .sixtyDayRule(report.isSixtyDaysRule() ? "Ja" : "Nej")
                    .distance(report.getDistance())
                    .amount(report.getAmountToReimburse())
                    .kmRateType(report.getKmRateType())
                    .kmRate(report.getKmRate() + " øre/km")
                    .status(report.getStatus().getText())
                    .approvedBy(report.getApprovedBy() != null ? report.getApprovedBy().getName() : null)
                    .timeOfApproval(report.getClosedDate())
                    .flagged60days(report.isFlaggedSixtyDayRule())
                    .build();

            licensePlates.add(report.getLicensePlate());
            reponseReportDTOs.add(reportDTO);
        }

        ResponseDTO res = new ResponseDTO(personService.getById(requestReportDTO.personId).getName(), orgUnitService.findById(requestReportDTO.orgUnitId).getLongDescription(), requestReportDTO.from, requestReportDTO.to, LocalDate.now(), licensePlates, reponseReportDTOs);

        return ResponseEntity.ok(res);
    }

    record WhoAmIDTO(long personId, String personName, Map<Long, String> orgUnits) {}
    @GetMapping("/reportCard/whoAmI")
    public ResponseEntity<?> retrieveInformationAboutLoggedInUser() {
        Person me = securityUtil.getPerson();
        List<Employment> employments = employmentService.getByPerson(me);
        Map<Long, String> orgs = new HashMap<>();
        for (Employment employment : employments) {
            orgs.put(employment.getOrgUnit().getId(), employment.getOrgUnit().getLongDescription());
        }
        return new ResponseEntity<>(new WhoAmIDTO(me.getId(), me.getName(), orgs), HttpStatus.OK);
    }

    record SelectableOrgUnitsDTO(Map<Long, String> orgUnits) {}
    @GetMapping("/reportCard/getOrgunitsByEmployment/{id}")
    public ResponseEntity<?> retrieveInformationAboutOrgUnits(@PathVariable("id") Long id) {

        List<Employment> employments = new ArrayList<>();
        Person selectedPerson = personService.getById(id);
        if (securityUtil.hasRole(Roles.ROLE_ADMINISTRATOR)) {
		    employments = employmentService.getByPerson(selectedPerson);
        }
        else if (securityUtil.hasRole(Roles.ROLE_APPROVER)) {
            employments = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now())).stream().filter(employment -> Objects.equals(employment.getPerson(), selectedPerson)).collect(Collectors.toList());
        }

		Map<Long, String> orgs = new HashMap<>();
        for (Employment employment : employments) {
            orgs.put(employment.getOrgUnit().getId(), employment.getOrgUnit().getLongDescription());
        }
        return new ResponseEntity<>(new SelectableOrgUnitsDTO(orgs), HttpStatus.OK);
    }

    @RequireApprover
    @GetMapping("/reportCard/whoDoILeadOrSub")
    public ResponseEntity<?> whoDoILeadOrSub() {
        Set<Employment> employments = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now()));
        Map<Long, String> orgs = new HashMap<>();
        for (Employment employment : employments) {
            orgs.put(employment.getOrgUnit().getId(), employment.getOrgUnit().getLongDescription());
        }
        return new ResponseEntity<>(orgs, HttpStatus.OK);
    }

    @PostMapping("/reportCard/downloadReport")
    public ResponseEntity<StreamingResponseBody> downloadReportSelection(HttpServletRequest request, HttpServletResponse response, @RequestBody RequestReportDTO dto) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"Rapporteringer.zip\"").body(out -> {
            var zipOutputStream = new ZipOutputStream(out);
            reportService.addFilesToZipCsv(dto.personId, dto.orgUnitId, dto.from, dto.to, zipOutputStream, "rapport");
            zipOutputStream.close();
        });
    }

    record Select2Result(long id, String text) {}
    record Select2Results(List<Select2Result> results) {}


    // Used by the select2
    @GetMapping("/reportCard/getPersonsByPrefix")
    public ResponseEntity<?> getPersons(@RequestParam("q") String prefix, @RequestParam(name = "orgUnitId", required = false, defaultValue = "") String orgUnitId) {
        List<Person> persons = personService.findStartingWith(prefix);

        if (!orgUnitId.isEmpty()) {
            var id = Long.parseLong(orgUnitId);
            persons = persons.stream()
                    .filter(p -> p.getEmployments().stream().anyMatch(e -> e.getOrgUnit().getId() == id))
                    .collect(Collectors.toList());
        }

        // Map persons to DTO
        List<Select2Result> results = new ArrayList<>();
        for (Person person : persons) {
            String personString = person.getName() + " (" + getEmployeeNumbers(person, 3) + ')';
            results.add(new Select2Result(person.getId(), personString));
        }

        // Sort
        if (!results.isEmpty()) {
            results.sort(Comparator.comparing(Select2Result::text, Comparator.nullsFirst(Comparator.naturalOrder())));
        }

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    @GetMapping("/reportCard/getOrgUnitsByPrefix")
    public ResponseEntity<?> getOrgUnits(@RequestParam("q") String prefix, @RequestParam("personId") String personId) {
		List<OrgUnit> orgUnits;

		if (StringUtils.hasText(personId)) {
			orgUnits = personService.getById(Long.parseLong(personId)).getEmployments().stream()
                    .map(Employment::getOrgUnit)
                    .filter(orgUnit -> {
                        String name = orgUnit.getLongDescription();
                        return isMatching(prefix, name);
                    })
                    .collect(Collectors.toSet())
                    .stream().toList();
        }
		else {
			orgUnits = orgUnitService.findStartingWith(prefix);
        }

        // Map persons to DTO
        List<Select2Result> results = new ArrayList<>();
        for (var orgUnit : orgUnits) {
            results.add(new Select2Result(orgUnit.getId(), orgUnit.getLongDescription()));
        }

        // Sort
        if (!results.isEmpty()) {
            results.sort(Comparator.comparing(Select2Result::text, Comparator.nullsFirst(Comparator.naturalOrder())));
        }

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    private static boolean isMatching(String prefix, String name) {
        if (!StringUtils.hasText(name)) {
            return true;
        }

        String lowerDesc = name.toLowerCase();
        String lowerPrefix = prefix.toLowerCase();

        // Check if starts with prefix
        if (lowerDesc.startsWith(lowerPrefix)) {
            return true;
        }

        // Check if second word starts with prefix (safely)
        String[] words = lowerDesc.split("\\s+");
        if (words.length > 1 && words[1].startsWith(lowerPrefix)) {
            return true;
        }

        return false;
    }

    // Used by the select2
    @GetMapping("/reportCard/getPersonsByPrefix/id")
    public ResponseEntity<?> getPerson(@RequestParam("q") String prefix) {
        Person person = personService.getById(Long.parseLong(prefix));
        String result = person.getName() + " (" + getEmployeeNumbers(person, 3) + ')';
        return new ResponseEntity<>(new Select2Result(person.getId(), result), HttpStatus.OK);
    }

    @GetMapping("/reportCard/getOrgUnitsByPrefix/id")
    public ResponseEntity<?> getOrgUnit(@RequestParam("q") String prefix) {
        var orgUnit = orgUnitService.findById(Long.parseLong(prefix));
        String result = orgUnit.getLongDescription();
        return new ResponseEntity<>(new Select2Result(orgUnit.getId(), result), HttpStatus.OK);
    }

    // Used by the select2
    @RequireApprover
    @GetMapping("reportCard/getPersonsByApprover")
    public ResponseEntity<?> getPersonsByApprover(@RequestParam("q") String prefix, @RequestParam("orgUnitId") String orgUnitId) {
        List<Select2Result> results = new ArrayList<>();
        if (!StringUtils.hasLength(prefix)) {
            return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
        }

        // Map evaluated employmentSet to list of persons that match the query
        Set<Employment> employmentSet = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now()));

        // TODO: fix
		if ("undefined".equals(orgUnitId)) {
			throw new AssertionError();
        }

        if (!orgUnitId.isEmpty()) {
            var id = Long.parseLong(orgUnitId);
            employmentSet = employmentSet.stream()
                .filter(employment -> employment.getOrgUnit().getId() == id)
                .collect(Collectors.toSet());
        }

        Set<Person> persons = employmentSet.stream()
                .map(Employment::getPerson)
                .filter(person -> person.getName().toLowerCase().contains(prefix.toLowerCase()))
                .collect(Collectors.toSet());

        // Convert to format expected by Select2 in frontend
        for (Person person : persons) {
            String result = person.getName() + " (" + getEmployeeNumbers(person, 3) + ')';
            results.add(new Select2Result(person.getId(), result));
        }

        // Sort with same "Token Match Score"-strategy we use other places in code
        Comparator<Select2Result> comparator = Comparator.comparingDouble(resultRow -> SearchUtil.calculateTokenMatchScore(resultRow.text, prefix));
        results = results.stream()
                .sorted(comparator.reversed()) // Sort by match score descending
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    // Used by the select2
    @RequireApprover
    @GetMapping("/reportCard/getPersonsByApprover/id")
    public ResponseEntity<?> getPersonsByApproverById(@RequestParam("q") String prefix) {
        List<Select2Result> results = new ArrayList<>();

        if (!StringUtils.hasLength(prefix)) {
            return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
        }

        // Map evaluated employmentSet to list of persons that match the query
        Set<Employment> employmentSet = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now()));
        Set<Person> persons = employmentSet.stream()
                .map(Employment::getPerson)
                .filter(person -> person.getId() == Long.parseLong(prefix.toLowerCase()))
                .collect(Collectors.toSet());

		if (persons.size() != 1) {
            return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
		}

        Person person = persons.stream().findFirst().get();
        String result = person.getName() + " (" + getEmployeeNumbers(person, 3) + ')';
        return new ResponseEntity<>(new Select2Result(person.getId(), result), HttpStatus.OK);
    }

    @RequireApprover
    @GetMapping("reportCard/getOrgUnitsByApprover")
    public ResponseEntity<?> getOrgUnitsByApprover(@RequestParam("q") String prefix, @RequestParam("personId") String personId) {
        List<Select2Result> results = new ArrayList<>();
        if (!StringUtils.hasLength(prefix)) {
            return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
        }

        // TODO: fix
		if ("undefined".equals(personId)) {
			throw new AssertionError();
		}

        var orgUnits = personId.isEmpty() ? orgUnitService.getAll()
            : personService.getById(Long.parseLong(personId)).getEmployments().stream()
                .filter(employment -> {
                    String name = employment.getOrgUnit().getLongDescription();
                    return isMatching(prefix, name);
                })
                .map(Employment::getOrgUnit)
                .collect(Collectors.toList());

        // Convert to format expected by Select2 in frontend
        for (var orgUnit : orgUnits) {
            results.add(new Select2Result(orgUnit.getId(), orgUnit.getLongDescription()));
        }

        Comparator<Select2Result> comparator = Comparator.comparingDouble(resultRow -> SearchUtil.calculateTokenMatchScore(resultRow.text, prefix));
        results = results.stream()
                .sorted(comparator.reversed()) // Sort by match score descending
                .collect(Collectors.toList());

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    // Used by the select2
    @RequireApprover
    @GetMapping("/reportCard/getOrgUnitsByApprover/id")
    public ResponseEntity<?> getOrgUnitsByApproverById(@RequestParam("q") String prefix) {
        List<Select2Result> results = new ArrayList<>();
        if (!StringUtils.hasLength(prefix)) {
            return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
        }

        var orgUnit = orgUnitService.findById(Long.parseLong(prefix));
        return new ResponseEntity<>(new Select2Result(orgUnit.getId(), orgUnit.getLongDescription()), HttpStatus.OK);
    }

    @PostMapping("/rest/personal/report/list")
    public DataTablesOutput<ReportView> paginatingTable(@RequestBody DataTablesInput input, @RequestParam(name = "status") ReportStatus status, @RequestParam(name = "startDate", required = false) LocalDate startDate, @RequestParam(name = "endDate", required = false) LocalDate endDate, @RequestParam(name = "orgUnitId", required = false) Long orgUnitId) {
        Person loggedInPerson = securityUtil.getPerson();
        if (loggedInPerson == null) {
            return null;
        }
        Specification<ReportView> spec = DatatableSpecBuilderUtil.getReportViewSpecification(status, startDate, endDate, loggedInPerson.getId(), null);
        return reportDatatableDao.findAll(input, spec);
    }

    record AvailableAddressResponse(List<AvailableAddressDTO> addresses, String deltaDistance, String maxDistanceToSubtract, String fourKmWithdrawn) {}
    record AvailableAddressDTO(String address, String description) {}
    @PostMapping("/rest/report/available-information")
    public ResponseEntity<?> getAvailableAddresses(@RequestBody(required = false) String date) {
        if (!StringUtils.hasLength(date)) {
            return ResponseEntity.badRequest().build();
        }

        Person loggedInPerson = securityUtil.getPerson();
        if (loggedInPerson == null) {
            return ResponseEntity.badRequest().build();
        }

        // Fetch previous subtracted distance for the date that the user is currently trying to create a report on.
        double deltaDistance = 0.00;
        double fourKmWithdrawn = 0.00;
        LocalDate driveDate = LocalDate.parse(date);
        List<Report> reportsFromGivenDay = reportService.getByPersonAndDriveDate(loggedInPerson, driveDate);
        if (!reportsFromGivenDay.isEmpty()) {
            for (Report report : reportsFromGivenDay) {
                // Round trip is not actually taken into account for rawDistance so we adjust the value for it
                if (report.isRoundTrip()) {
                    deltaDistance += ((report.getRawDistance() * 2) - report.getDistance());
                }
                else {
                    deltaDistance += (report.getRawDistance() - report.getDistance());
                }

				if (report.isFourKmRule()) {
                    boolean roundTrip = report.isRoundTrip();
                    double rawDistance = report.getRawDistance();

                    double fullDistance = rawDistance * ((roundTrip) ? 2 : 1);
                    double distance = report.getDistance();

                    double homeToBorderDistance = 0;

                    if (!CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE.equals(report.getCalculationType())) {
                        if (report.isStartsAtHome()) {
                            homeToBorderDistance += report.getHomeToBorderDistance();
                        }
                        if (report.isEndsAtHome()) {
                            homeToBorderDistance += report.getHomeToBorderDistance();
                        }
                    }

                    double totalAfterHTBWithDraw = fullDistance - homeToBorderDistance;
                    fourKmWithdrawn += Double.min(totalAfterHTBWithDraw - distance, 4);
                }
            }

        }
        ArrayList<Employment> employments = loggedInPerson.getEmployments().stream()
                .filter(Objects::nonNull)
                .filter(employment -> employment.getStartDate() == null || employment.getStartDate().minusDays(1).isBefore(driveDate.atStartOfDay()))
                .filter(employment -> employment.getStopDate() == null || employment.getStopDate().plusDays(1).isBefore(driveDate.atStartOfDay()))
                .collect(Collectors.toCollection(ArrayList::new));
		double maxDistanceToSubtract = calculatorService.getMaxDistanceToSubtractByAllEmployments(employments, driveDate);

        try {
            List<AvailableAddressDTO> availableAddressDTOS = fetchAvailableAddressesDTOByDate(date, loggedInPerson);


            DecimalFormat df = new DecimalFormat("####0.##");
            DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
            decimalFormatSymbols.setDecimalSeparator('.');
            decimalFormatSymbols.setGroupingSeparator(',');
            df.setDecimalFormatSymbols(decimalFormatSymbols);
            return ResponseEntity.ok(new AvailableAddressResponse(availableAddressDTOS, df.format(deltaDistance), df.format(maxDistanceToSubtract), df.format(Double.min(fourKmWithdrawn, 4))));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @NotNull
    private List<AvailableAddressDTO> fetchAvailableAddressesDTOByDate(String date, Person loggedInPerson) {
        LocalDate parsedDate = LocalDate.parse(date);
        List<Address> availableAddressesByDate = addressService.getAvailableAddressesByDate(loggedInPerson, parsedDate.atStartOfDay());
        List<AvailableAddressDTO> collect = availableAddressesByDate.stream()
                .map(address -> {
                    Address addressOrDeviating = addressService.getDeviations(loggedInPerson, address, parsedDate.atStartOfDay());
                    return new AvailableAddressDTO(addressOrDeviating.getAddressString(), addressOrDeviating.getFullDescription());
                })
                .collect(Collectors.toCollection(ArrayList::new));

        collect.sort(Comparator.comparing(AvailableAddressDTO::address));
        return collect;
    }

    record PreEmptiveInformationRecord(CalculationType calculationType, LocalDate driveDate, List<AddressRecord> addressRecords, double distance, boolean roundTrip, boolean startsOrEndsHomeForRead, double maxDistanceToSubtract, double alreadySubtracted, boolean fourKmRuleEnabled, double homeToBorderDistance, double fourKmWithdrawn) {}
    @PostMapping("rest/report/calculateDeltaDistance")
    public ResponseEntity<?> getDeltaDistance(@RequestBody PreEmptiveInformationRecord record) {
        double distance = record.distance;
        if (record.roundTrip()) {
            distance *= 2;
        }

        List<GpsCoordinate> coords = new ArrayList<>();
        if (record.addressRecords != null && !record.addressRecords.isEmpty()) {
            for (int i = 0; i < record.addressRecords.size(); i++) {
                AddressRecord addr = record.addressRecords.get(i);
                GpsCoordinate gps = new GpsCoordinate();
                gps.setLatitude(addr.lat);
                gps.setLongitude(addr.lng);
                gps.setAddress(addr.road + " " + addr.house_number + ", " + addr.postcode + " " + addr.town);
                gps.setWaypoint(true);
                gps.setPointNumber(i);
                coords.add(gps);
            }
            GpsCoordinate first = coords.getFirst();
            first.setWaypoint(false);
            first.setStartPoint(true);

			if (record.roundTrip) {
				first.setEndPoint(true);
			}
            else {
                GpsCoordinate last = coords.getLast();
                last.setWaypoint(false);
                last.setEndPoint(true);
            }

        }
        double deltaDistance = calculatorService.calculatePotentialDeltaDistance(record.calculationType, record.driveDate, coords, distance, record.startsOrEndsHomeForRead, record.maxDistanceToSubtract, record.alreadySubtracted, record.fourKmRuleEnabled, record.homeToBorderDistance, record.roundTrip, record.fourKmWithdrawn);

        return new ResponseEntity<>(deltaDistance, HttpStatus.OK);
    }

    record SearchInKnownAddressesOutput(AddressType type, long id, String addressString, String description, String streetName, String streetNumber, int zipCode, String town, double longitude, double latitude) {}
    record SearchAddressRequest(String input, LocalDate date) {}
    @PostMapping("/rest/report/addresses/search")
    public ResponseEntity<?> searchInKnownAddresses(@RequestBody SearchAddressRequest request) {
        ArrayList<Address> availableAddressesByDate = addressService.getAvailableAddressesByDate(securityUtil.getPerson(), request.date.atStartOfDay());
        List<Address> filteredAddresses = addressService.filterAddressesByInput(availableAddressesByDate, request.input);

        return new ResponseEntity<>(mapAddressesToDTO(filteredAddresses), HttpStatus.OK);
    }

    @PostMapping("/rest/report/addresses/search/init")
    public ResponseEntity<?> initSearch(@RequestBody SearchAddressRequest request) {
		if (request.date == null) {
            // Possible invalid date
            log.warn("Date was null, possible invalid date");
            return ResponseEntity.badRequest().build();
        }

        ArrayList<Address> availableAddressesByDate = addressService.getAvailableAddressesByDate(securityUtil.getPerson(), request.date.atStartOfDay());
        return new ResponseEntity<>(mapAddressesToDTO(availableAddressesByDate), HttpStatus.OK);
    }

    @NotNull
    private static ArrayList<SearchInKnownAddressesOutput> mapAddressesToDTO(List<Address> addresses) {
        return addresses.stream().map(address -> {
            StringBuilder sb = new StringBuilder();
            String desc = address.getDescription();
            AddressType addressType = address.getType();

            boolean descriptionPresent = StringUtils.hasLength(desc);
            switch (addressType) {
                case HOME -> {
                    if (descriptionPresent) {
                        sb.append(desc).append("; ");
                    }
                    sb.append(addressType.getDescription());
                }
                case WORK -> {
                    if (descriptionPresent) {
                        sb.append(desc).append("; ");
                    }
                    sb.append(address.getOrgUnit().getLongDescription());
                }
                case STANDARD, ALTERNATIVE, DHOME, DWORK -> {
                    if (descriptionPresent) {
                        sb.append(desc);
                    }
                }
            }

            return new SearchInKnownAddressesOutput(address.getType(),
                    address.getId(),
                    address.getAddressString(),
                    sb.toString(),
                    address.getStreetName(),
                    address.getStreetNumber(),
                    address.getZipCode(),
                    address.getTown(),
                    address.getLongitude(),
                    address.getLatitude()
            );
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private String getEmployeeNumbers(Person person, int count) {
        List<Employment> employments = person.getEmployments();

        String result = "";
        if (!employments.isEmpty()) {
            for (int i = 0; i < employments.size() && i < count; i++) {
                result = result.concat(employments.get(i).getEmployeeNumber());
                if (i == employments.size() - 1 || i == count - 1) {
                    break;
                }

                result = result.concat(", ");
            }
        }
        return result;
    }
}
