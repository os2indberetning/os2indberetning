package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.emailService.service.EmailQueueService;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.dao.ReportDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
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
    private final OS2indberetningConfiguration configuration;
    private final SubstituteService substituteService;
    private final EmailQueueService emailQueueService;

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

                headerBuilder.append("\"").append("Dato for kørsel").append("\";");
                headerBuilder.append("\"").append("Dato for indberetning").append("\";");
                headerBuilder.append("\"").append("Medarbejder").append("\";");
                headerBuilder.append("\"").append("MA. NR.").append("\";");
                headerBuilder.append("\"").append("Org. Enhed").append("\";");
                headerBuilder.append("\"").append("Formål").append("\";");
                headerBuilder.append("\"").append("Rute").append("\";");
                headerBuilder.append("\"").append("Retur").append("\";");
                headerBuilder.append("\"").append("MK").append("\";");
                headerBuilder.append("\"").append("4-km").append("\";");
                headerBuilder.append("\"").append("Km til kommunegrænse").append("\";");
                headerBuilder.append("\"").append("60-dage").append("\";");
                headerBuilder.append("\"").append("KM til udbetaling").append("\";");
                headerBuilder.append("\"").append("Beløb").append("\";");
                headerBuilder.append("\"").append("Taksttype").append("\";");
                headerBuilder.append("\"").append("Takst").append("\";");
                headerBuilder.append("\"").append("Status").append("\";");
                headerBuilder.append("\"").append("Godkendt/Afvist dato").append("\";");
                headerBuilder.append("\"").append("Godkendt/Afvist af").append("\"\n");
                zipOutputStream.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));

                for (Report report : reports) {
                    StringBuilder rowBuilder = new StringBuilder();
                    rowBuilder.append("\"").append(report.getDriveDate()).append("\";");
                    rowBuilder.append("\"").append(report.getCreatedDate().toLocalDate()).append("\";");
                    rowBuilder.append("\"").append(report.getPerson() != null ? report.getPerson().getName() : "").append("\";");
                    rowBuilder.append("\"").append(report.getEmployeeNumber()).append("\";");
                    rowBuilder.append("\"").append(report.getEmployment().getOrgUnit().getLongDescription()).append("\";");
                    rowBuilder.append("\"").append(report.getPurpose()).append("\";");
                    rowBuilder.append("\"").append(report.getAddressesString().replace('\n', ' ')).append("\";");
                    rowBuilder.append("\"").append(report.isRoundTrip() ? "Ja" : "Nej").append("\";");
                    rowBuilder.append("\"").append(report.isExtraDistance() ? "Ja" : "Nej").append("\";");
                    rowBuilder.append("\"").append(report.isFourKmRule() ? "Ja" : "Nej").append("\";");
                    rowBuilder.append("\"").append(report.getHomeToBorderDistance()).append("\";");
                    rowBuilder.append("\"").append(report.isSixtyDaysRule() ? "Ja" : "Nej").append("\";");
                    rowBuilder.append("\"").append(roundValue(report.getDistance())).append("\";");
                    rowBuilder.append("\"").append(roundValue(report.getAmountToReimburse())).append("\";");
                    rowBuilder.append("\"").append(report.getKmRateType()).append("\";");
                    rowBuilder.append("\"").append(roundValue(report.getKmRate())).append(" øre/km").append("\";");
                    rowBuilder.append("\"").append(report.getStatus().getText()).append("\";");
                    rowBuilder.append("\"").append(report.getClosedDate() != null ? report.getClosedDate().toLocalDate() : "").append("\";");
                    rowBuilder.append("\"").append(report.getApprovedBy() != null ? report.getApprovedBy().getName() : "").append("\"\n");
                    zipOutputStream.write(rowBuilder.toString().getBytes(StandardCharsets.UTF_8));
                }
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
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#########0.##", decimalFormatSymbols);
        decimalFormat.setRoundingMode(RoundingMode.DOWN); // Truncate no rounding, already done
        return decimalFormat.format(roundedValue);
    }

    public List<Report> getByProcessedDate(LocalDateTime processedDate) { return reportDao.findByProcessedDate(processedDate); }

    public List<Report> getByActualLeader(Person person) { return reportDao.findByActualLeaderId(person); }

    public List<Report> getByApprovedBy(Person person) { return reportDao.findByApprovedById(person); }

    public List<Report> getByPerson(Person person) { return reportDao.findByPersonId(person); }
    
    public List<Report> getByPersonAndStatusAndPage(Person person, ReportStatus status, int pageNr) {
        Pageable pageable = PageRequest.of(pageNr, 20);
        return reportDao.findByPersonAndStatus(person, status, pageable);
    }
    
    public List<Report> getByPersonAndDriveDate(Person person, LocalDate date) { return reportDao.findByPersonAndDriveDate(person, date); }

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
        return result;
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
            List<Substitute> subWithOrgUnits = substituteService.findWhoSubsPerson(leader, orgUnit, report.getDriveDate());
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

    @Transactional
    public void updateApproversForPending() {
        List<Report> allPending = getByStatus(ReportStatus.PENDING);
        List<Report> allUpdated = new ArrayList<>();

        for (Report report : allPending) {
            String allApproversString = null;
            List<Person> allApprovers = new ArrayList<>(findApprovers(report));
            if (!allApprovers.isEmpty()) {
                StringBuilder sb = new StringBuilder(allApprovers.removeFirst().getName());
                for (Person approver : allApprovers) {
                    sb.append(", ").append(approver.getName());
                }
                allApproversString = sb.toString();
            }

            if (!Objects.equals(report.getPotentialApprovers(), allApproversString)) {
                report.setPotentialApprovers(allApproversString);
                allUpdated.add(report);
            }
        }

        if (!allUpdated.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Updated the approvers of {} reports!", allUpdated.size());
            }
            saveAll(allUpdated);
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
}