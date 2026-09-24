package dk.digitalidentity.indberetning.service;

import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import dk.digitalidentity.emailService.service.EmailQueueService;
import dk.digitalidentity.indberetning.model.dao.ReportDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.Route;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final ReportDao reportDao;
    private final RateTypeService rateTypeService;
    private final SecurityUtil securityUtil;
    private final EmploymentService employmentService;
    private final PersonService personService;
    private final OrgUnitService orgUnitService;
    private final SubstituteService substituteService;
    private final EmailQueueService emailQueueService;
	private final RouteService routeService;
	private final ReportDaoTransactionService reportDaoTransactionService;

	public List<Report> getByIdIn(List<Long> reportIds) {
        return reportDao.findByIdIn(reportIds);
    }

    public List<Report> getByIdInAndStatus(List<Long> reportIds, ReportStatus status) {
        return reportDao.findByIdInAndStatus(reportIds, status);
    }

    public List<Report> getByPersonIdIn(List<Long> personIds) {
        return reportDao.findByPerson_IdIn(personIds);
    }

    public List<Report> getByPersonIdInAndStatus(List<Long> personIds, ReportStatus status) {
        return reportDao.findByPerson_IdInAndStatus(personIds, status);
    }

    public List<Report> getByEmploymentIdIn(List<Long> employmentIds) {
        return reportDao.findByEmployment_IdIn(employmentIds);
    }

    public List<Report> getByEmploymentIdAndStatus(List<Long> employmentIds, ReportStatus status) {
        return reportDao.findByEmployment_IdInAndStatus(employmentIds, status);
    }

    public void populatePersonalReportList(Model model, ReportStatus status) {
        populateReportListModel(model, getByPersonAndStatus(securityUtil.getPerson(), status), status);
    }

    public List<Report> getByPersonAndNotificationNotSentAndStatusRejected(Person person) {
        return reportDao.findByPersonAndNotificationSentFalseAndStatus(person, ReportStatus.REJECTED);
    }

    public void populateApproveReportList(Model model, ReportStatus status) {
        // TODO this method currently does not support substitutes
        //  and will only give a list of reports from OUs
        //  of which the logged in person has an employment with leader=true

        Person person = securityUtil.getPerson();
        List<OrgUnit> leaderOfOUs = person.getEmployments().stream()
                .filter(Employment::isLeader)
                .map(Employment::getOrgUnit)
                .collect(Collectors.toList());

        // Find all reports associated with OUs that the person is a leader of
        // EXCEPT for reports made by the leader themselves.
        List<Report> filteredReports = reportDao.findByEmployment_OrgUnitIn(leaderOfOUs).stream()
                .filter(report -> !Objects.equals(report.getPerson(), person))
                .filter(report -> Objects.equals(status, report.getStatus()))
                .toList();

        populateReportListModel(model, filteredReports, status);
    }

    public void populateAdminReportList(Model model, ReportStatus status) {
        populateReportListModel(model, getByStatus(status), status);
    }

    private void populateReportListModel(Model model, List<Report> reports, ReportStatus status) {
        // Lists are immutable, have to reassign
        if (!reports.isEmpty()) {
            reports = new ArrayList<>(reports);
            reports.sort(Comparator.comparing(Report::getDriveDate, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        }
        model.addAttribute("prefix", status.name());
        model.addAttribute("reports", reports);
        model.addAttribute("status", status);
        model.addAttribute("rateTypes", rateTypeService.getAll());
    }

    public void addFilesToZipCsv(long personId, long orgUnitId, LocalDate from, LocalDate to, ZipOutputStream zipOutputStream, String fileNamePrefix) {
        try {
            log.info("Starting rendering logs from " + from + " to " + to);
            List<Employment> employments = employmentService.getByPersonAndOrgUnit(personService.getById(personId), orgUnitService.findById(orgUnitId));
            List<Report> reports = new ArrayList<>();
            for (Employment employment : employments) {
				reports.addAll(getByPersonAndOrgUnitAndDriveDate(personService.getById(personId), employment.getEmployeeNumber(), from, to));
			}

            if (!reports.isEmpty()) {
                // one file per person
                String fileName = fileNamePrefix + " " + from + "-" + to + ".csv";
                ZipEntry taskfile = new ZipEntry(fileName);
                zipOutputStream.putNextEntry(taskfile);

                StringBuilder headerBuilder = new StringBuilder();

                // Leave a BOM, it tells excel that its UTF-8 encoding.
                zipOutputStream.write(0xEF);
                zipOutputStream.write(0xBB);
                zipOutputStream.write(0xBF);

				CSVFormat format = CSVFormat.EXCEL.builder()
						.setDelimiter(";")
						.setHeader(
								"Dato for kørsel",
								"Dato for indberetning",
								"Medarbejder",
								"MA. NR.",
								"Org. Enhed",
								"Formål",
								"Rute",
								"Retur",
								"MK",
								"4-km",
								"Km til kommunegrænse",
								"60-dage",
								"KM til udbetaling",
								"Beløb",
								"Taksttype",
								"Takst",
								"Status",
								"Godkendt/Afvist dato",
								"Godkendt/Afvist af"
						)
						.get();

				OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
				CSVPrinter printer = new CSVPrinter(writer, format);

				for (Report report : reports) {
					printer.printRecord(
							report.getDriveDate(),
							report.getCreatedDate().toLocalDate(),
							report.getPerson() != null ? report.getPerson().getName() : "",
							report.getEmployeeNumber(),
							report.getEmployment().getOrgUnit().getLongDescription(),
							report.getPurpose(),
							report.getAddressesString().replace('\n', ' '),
							report.isRoundTrip() ? "Ja" : "Nej",
							report.isExtraDistance() ? "Ja" : "Nej",
							report.isFourKmRule() ? "Ja" : "Nej",
							roundValue(report.getHomeToBorderDistance()),
							report.isSixtyDaysRule() ? "Ja" : "Nej",
							roundValue(report.getDistance()),
							roundValue(report.getAmountToReimburse()),
							report.getKmRateType(),
							roundValue(report.getKmRate()) + " øre/km",
							report.getStatus().getText(),
							report.getClosedDate() != null ? report.getClosedDate().toLocalDate() : "",
							report.getApprovedBy() != null ? report.getApprovedBy().getName() : ""
					);
				}

				printer.flush();
				zipOutputStream.closeEntry();
            }

            log.info("Done rendering logs from " + from + " to " + to);
        } catch (Exception ex) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedFrom = from.format(formatter);
            String formattedTo = to.format(formatter);
            log.warn("Failed to add file for reports from " + formattedFrom + " to " + formattedTo + " to zip. Error code: " + ex);
        }

        log.info("Done rendering logs for download");
    }

    private static String roundValue(double distance) {
        double roundedValue = DoubleUtil.round(distance);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator(',');
        DecimalFormat decimalFormat = new DecimalFormat("#########0.##", decimalFormatSymbols);
        decimalFormat.setRoundingMode(RoundingMode.DOWN); // Truncate no rounding, already done
        return decimalFormat.format(roundedValue);
    }

    public List<Report> getByProcessedDate(LocalDateTime processedDate) { return reportDao.findByProcessedDate(processedDate); }

    public List<Report> getByActualLeader(Person person) { return reportDao.findByActualLeaderId(person); }

    public List<Report> getByApprovedBy(Person person) { return reportDao.findByApprovedById(person); }

    public List<Report> getByPerson(Person person) { return reportDao.findByPersonId(person); }

    public boolean hasReports(Person person) {
        return reportDao.existsByPerson(person);
    }

    public boolean hasReports(Employment employment) {
        return reportDao.existsByEmployment(employment);
    }

    public List<Report> getByPersonAndStatusAndPage(Person person, ReportStatus status, int pageNr) {
        Pageable pageable = PageRequest.of(pageNr, 20);
        return reportDao.findByPersonAndStatus(person, status, pageable);
    }
    
    public List<Report> getByPersonAndDriveDate(Person person, LocalDate date) { return reportDao.findByPersonAndDriveDate(person, date); }

    public List<Report> getByPersonAndDriveDateAndStatusNotEqual(Person person, LocalDate date, ReportStatus status, ReportStatus status2) {
        return reportDao.findByPersonAndDriveDateAndStatusNotAndStatusNot(person, date, status, status2);
    }

    public void delete(long id) {
        reportDao.deleteById(id);
    }

    public List<Report> getByPersonAndStatus(Person person, ReportStatus status) {
        return reportDao.findByPersonAndStatus(person, status);
    }

    public List<Report> getByStatus(ReportStatus status) {
        return reportDao.findByStatus(status);
    }

    public List<Report> getUnprocessed() {
        List<Report> unProcessed = reportDao.findByStatusAndProcessedDateNull(ReportStatus.ACCEPTED);
        List<Report> toBeRefunded = reportDao.findByStatus(ReportStatus.REJECTED_AFTER_INVOICE);
        List<Report> result = new ArrayList<>();
        result.addAll(unProcessed);
        result.addAll(toBeRefunded);
        ArrayList<Report> collect = result.stream().filter(report -> report.getErrorLog() == null).collect(Collectors.toCollection(ArrayList::new));
        return collect;
    }

    public Report save(Report report) {
        return reportDao.save(report);
    }

    public List<Report> saveAll(Collection<Report> reports) {
        return reportDao.saveAll(reports);
    }

    public List<Report> getAll() {
        return reportDao.findAll();
    }

    public Report getById(long id) {
        return reportDao.findById(id).orElse(null);
    }


    public List<Report> getByPersonAndOrgUnitAndDriveDate(Person person, String employeeNumber, LocalDate from, LocalDate to) {
        return reportDao.findByPerson_IdAndEmployeeNumberAndDriveDateBetween(person.getId(), employeeNumber, from, to);
    }

    public List<Report> getByPersonAndAddressAndDriveDateBetween(String stopAddress, LocalDate driveDateStart, LocalDate driveDateEnd, Person person) {
        return reportDao.findByPersonAndDriveDateBetweenAndCoords_EndPointTrueAndCoords_Address(person, driveDateStart, driveDateEnd, stopAddress);
    }

    public List<Report> getToBeRecalculated() {
        return reportDao.findByRecalculateTrue();
    }

    public List<Report> getByPersonAndDriveDateBetweenAndStatusPending(Person person, LocalDate driveDateStart, LocalDate driveDateEnd) {
        return reportDao.findByPersonAndDriveDateBetweenAndStatus(person, driveDateStart, driveDateEnd, ReportStatus.PENDING);
    }

    public List<Report> findByDistinctPersonAndReportStatus(Person person, ReportStatus reportStatus) {
        return reportDao.findByDistinctPerson(person, reportStatus);
    }

    public Optional<Report> findLatestReportByPerson(Person person) {
        return reportDao.findFirstByPersonOrderByCreatedDateDesc(person);
    }

    public Set<Person> findAllApproversForReports(List<Report> reports) {
        Set<Person> allApprovers = new HashSet<>();

        for (Report report : reports) {
            allApprovers.addAll(findApprovers(report));
		}
        return allApprovers;
    }

    public Set<Person> findApprovers(Report report) {
        HashSet<Person> approvers = new HashSet<>();

        OrgUnit orgUnit = report.getEmployment().getOrgUnit();
        OrgUnit reportOrgUnit = orgUnit;
        Person leader = orgUnitService.findLeaderOfOrgUnit(orgUnit);

        // If a report is made by the leader OR if no direct leader is found on OU,
        // step up through the parent OUs to find the nearest leader
        if (leader == null || Objects.equals(leader, report.getPerson())) {
            OrgUnit parent = orgUnit.getParent();
            OrgUnitService.OuTreeLeaderResult result = parent != null ? orgUnitService.findLeaderOfOrgUnitTree(parent) : null;
            orgUnit = result != null ? result.leadersOU() : null;
            leader = result != null ? result.leader() : null;
        }
        List<Substitute> personalApprovers = substituteService.findWhoPersonallyApprovesPerson(report.getPerson(), report.getDriveDate());
        personalApprovers = personalApprovers.stream()
                .filter(substitute -> substitute.getEndDate() == null || substitute.getEndDate().isAfter(report.getDriveDate()))
                .toList();

        // IF any personal approvers has been assigned to a person
        // they are the only ones responsible for approving.
        if (!personalApprovers.isEmpty()) {
            approvers.addAll(personalApprovers.stream().map(Substitute::getSubstitute).toList());
            approvers.remove(report.getPerson());
            return approvers;
        }
        else if (leader != null && orgUnit != null) {
            // We are now checking the leader we found above (either direct leader or nearest)
            // and seeing if any substitutes has been defined for them.
            // Use the report's original OU, not the leader's OU, since substitutes are assigned
            // to the OU where the report was made (which may differ when walking up the hierarchy).
            List<Substitute> subWithOrgUnits = substituteService.findWhoSubsPerson(leader, reportOrgUnit, report.getDriveDate());
            subWithOrgUnits = subWithOrgUnits.stream()
                    .filter(substitute -> substitute.getEndDate() == null || substitute.getEndDate().isAfter(report.getDriveDate()))
                    .toList();

            // IF any substitute is set as SubExclusiveMode the leaders does not get added to the list
            if (subWithOrgUnits.stream().noneMatch(Substitute::isSubstituteExclusiveMode)) {
                approvers.add(leader);
            }

            approvers.addAll(subWithOrgUnits.stream().map(Substitute::getSubstitute).toList());
        }

        approvers.remove(report.getPerson());

        // Sanity check
        if (approvers.isEmpty()) {
            // Person has no:
            //   * leader
            //   * substitute for the leader
            //   * personal approver.
            log.warn("There is no approver for {}", report.getPerson().getName());
        }
        return approvers;
    }

	@Transactional(readOnly = true)
	public void updateApproversForPending() {
		List<Report> allPending = getByStatus(ReportStatus.PENDING);
		List<Report> allUpdated = new ArrayList<>();

		log.info("Starting approver update for {} pending reports", allPending.size());

		int count = 0;
		for (Report report : allPending) {
			if (++count % 500 == 0) {
				log.info("Processed " + count + " reports");
			}

			// Log initial state of each report
			Long initialRouteId = report.getRoute() != null ? report.getRoute().getId() : null;
			
			log.debug("Processing report ID: {}, route_id: {}, current approvers: '{}'", report.getId(), initialRouteId, report.getPotentialApprovers());

			String allApproversString = null;
			List<Person> allApprovers = new ArrayList<>(findApprovers(report));
			if (!allApprovers.isEmpty()) {
				// ensure the string is always generated in the same way
				allApprovers.sort(Comparator.comparing(Person::getName));

				StringBuilder sb = new StringBuilder(allApprovers.removeFirst().getName());
				for (Person approver : allApprovers) {
					sb.append(", ").append(approver.getName());
				}
				allApproversString = sb.toString();
			}

			if (!Objects.equals(report.getPotentialApprovers(), allApproversString)) {
				log.info("Report {} approvers changing from '{}' to '{}'", report.getId(), report.getPotentialApprovers(), allApproversString);

				report.setPotentialApprovers(allApproversString);
				allUpdated.add(report);
			}
		}

		if (!allUpdated.isEmpty()) {
			log.info("About to save {} updated reports", allUpdated.size());

			// Critical logging right before save - no try-catch, let it fail naturally
			for (Report report : allUpdated) {
				Long routeId = report.getRoute() != null ? report.getRoute().getId() : null;
				boolean routeExists = false;
				if (routeId != null) {
					routeExists = routeService.existsById(routeId);
				}

				log.info("Report {} - route_id: {}, route_exists: {}, new_approvers: '{}'", report.getId(), routeId, routeExists, report.getPotentialApprovers());
			}

			// Check for orphaned route references right before save
			List<Long> routeIds = allUpdated.stream()
					.map(r -> r.getRoute() != null ? r.getRoute().getId() : null)
					.filter(Objects::nonNull)
					.distinct()
					.toList();

			if (!routeIds.isEmpty()) {
				List<Long> existingRouteIds = routeService.findAllById(routeIds)
						.stream()
						.map(Route::getId)
						.toList();

				List<Long> missingRouteIds = routeIds.stream()
						.filter(id -> !existingRouteIds.contains(id))
						.toList();

				if (!missingRouteIds.isEmpty()) {
					log.info("About to save reports with non-existent routes: {}", missingRouteIds);

					// Log which specific reports reference missing routes
					allUpdated.stream()
							.filter(r -> r.getRoute() != null && missingRouteIds.contains(r.getRoute().getId()))
							.forEach(r -> log.info("Report {} references missing route {}",
									r.getId(), r.getRoute().getId()));
				}
			}

			log.info("Executing saveAll for reports: {}", allUpdated.stream().map(Report::getId).toList());

			if (log.isDebugEnabled()) {
				log.debug("Updated the approvers of {} reports!", allUpdated.size());
			}

			reportDaoTransactionService.saveAllWithTransaction(allUpdated);
		}

		substituteService.setChanged(false);
	}

    @Transactional
    public void sendRejectedEmails() {
        List<Person> persons = personService.findByReceiveEmailTrueAndReceivePersonalMailTrue();

        List<Report> allRecentRejectedReports = reportDao.findByStatusAndClosedDateAfterAndNotificationSentFalse(ReportStatus.REJECTED, LocalDateTime.now().minusMonths(3));
        Map<Person, Set<Report>> personMap = new HashMap<>();
        for (Report report : allRecentRejectedReports) {
			if (!persons.contains(report.getPerson()) || !StringUtils.hasText(report.getPerson().getEmail())) {
				continue;
			}

            if (!personMap.containsKey(report.getPerson())) {
                personMap.put(report.getPerson(), new HashSet<>());
            }

            personMap.get(report.getPerson()).add(report);
		}

        String emailHeader = "OS2Indberetning: En eller flere indberetninger afvist";
        for (Map.Entry<Person, Set<Report>> entry : personMap.entrySet()) {
            Person person = entry.getKey();

            // We are only interested in reports that have been rejected prior and not been notified about
            List<Report> reports = entry.getValue().stream()
                    .sorted(Comparator.comparing(Report::getDriveDate))
                    .toList();

            // No reports, Skip
            if (reports.isEmpty()) {
                continue;
            }

            StringBuilder emailText = new StringBuilder("Kære " + person.getName() + "<br><br>Følgende indberetninger er blevet afvist: <br>");

            for (Report report : reports) {
                emailText.append(report.getDriveDate()).append(": ").append(report.getPurpose());

                if (StringUtils.hasText(report.getUserComment())) {
                    emailText.append(" (Begrundelse: ").append(report.getUserComment()).append(")");
                }

                emailText.append("<br>");
                report.setNotificationSent(true);
            }
            // Don't forget to update the reports in the backend
            saveAll(reports);

            emailText.append("<br>").append("Tjek OS2Indberetning for flere oplysninger.");

            if (log.isDebugEnabled()) {
                log.debug("Sent email to: {} on email: {}", person.getName(), person.getEmail());
                log.debug("With a total of {} reports", reports.size());
            }

            emailQueueService.queueEmail(person.getEmail(), emailHeader, emailText.toString());
        }
    }

    public List<Report> findByProcessedDateBefore(LocalDateTime date) {
        return reportDao.findByProcessedDateBefore(date);
    }

    public void deleteAll(List<Report> reports) {
        reportDao.deleteAll(reports);
    }

    public List<Report> checkForExpiredReports() {
		LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
		return reportDao.findByCreatedDateBeforeAndProcessedDateBeforeAndClosedDateBeforeAndDriveDateBefore(fiveYearsAgo, fiveYearsAgo, fiveYearsAgo, fiveYearsAgo.toLocalDate());
	}

	public Page<Report> getByStatusesAndDriveDateBetween(Collection<ReportStatus> statuses, LocalDate from, LocalDate to, Long orgUnitId, Pageable pageable) {
		Page<Long> idPage;
		if (orgUnitId != null) {
			idPage = reportDao.findIdsByStatusInAndDriveDateBetweenAndOrgUnit(statuses, from, to, orgUnitId, pageable);
		} else {
			idPage = reportDao.findIdsByStatusInAndDriveDateBetween(statuses, from, to, pageable);
		}

		List<Long> ids = idPage.getContent();
		List<Report> reports = ids.isEmpty() ? List.of() : reportDao.findWithCoordsByIdIn(ids);
		return new PageImpl<>(reports, pageable, idPage.getTotalElements());
	}

	public Set<Long> getRouteReportIds(Collection<Long> reportIds) {
		if (reportIds.isEmpty()) {
			return Set.of();
		}
		return reportDao.findRouteReportIds(reportIds);
	}
}
