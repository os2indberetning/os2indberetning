package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ApiAccessDao;
import dk.digitalidentity.indberetning.model.entity.ApiAccess;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ApiType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InternalAPIService {

	private final ReportService reportService;
	private final ApiAccessDao apiAccessDao;
	private final StatisticService statisticService;

	public List<Long> recalculateReports(boolean dryRun, List<Long> reportIds, List<Long> personIds, List<Long> employmentIds, boolean allowChangeNonPending, boolean requireReApprove, LocalDate driveDateStart, LocalDate driveDateEnd) {
		Set<Report> toBeChanged = getToBeChanged(reportIds, personIds, employmentIds, allowChangeNonPending);
		toBeChanged = filterByDriveDate(driveDateStart, driveDateEnd, toBeChanged);

		if (!dryRun) {
			for (Report report : toBeChanged) {
				report.setRecalculate(true);

				if (allowChangeNonPending) {
					ReportStatus status = report.getStatus();
					if (ReportStatus.INVOICED.equals(status) || ReportStatus.ACCEPTED.equals(status)) {
						report.setStatus(requireReApprove ? ReportStatus.PENDING : ReportStatus.ACCEPTED);
					}
				}
			}

			reportService.saveAll(toBeChanged);
		}

		return toBeChanged.stream().map(Report::getId).toList();
	}

	private static Set<Report> filterByDriveDate(LocalDate driveDateStart, LocalDate driveDateEnd, Set<Report> toBeChanged) {
		int initialSize = toBeChanged.size();
		if (driveDateStart != null && driveDateEnd != null) {
			toBeChanged = toBeChanged.stream()
				.filter(report -> report.getDriveDate().isEqual(driveDateStart) ||
						(report.getDriveDate().isAfter(driveDateStart) && report.getDriveDate().isBefore(driveDateEnd)) ||
						report.getDriveDate().isEqual(driveDateEnd)).collect(Collectors.toSet());
		}
		int filteredSize = toBeChanged.size();
		log.info("Filtered reports to be recalculated from {} to {} reports", initialSize, filteredSize);
		return toBeChanged;
	}

	@NotNull
	private Set<Report> getToBeChanged(List<Long> reportIds, List<Long> personIds, List<Long> employmentIds, boolean allowChangeNonPending) {
		Set<Report> toBeChanged = new HashSet<>();

		if (reportIds != null && !reportIds.isEmpty()) {
			List<Report> reportsByIds = allowChangeNonPending ? reportService.getByIdIn(reportIds) : reportService.getByIdInAndStatus(reportIds, ReportStatus.PENDING);
			log.info("Found {} reports by Ids, marking for recalculation", reportsByIds.size());
			toBeChanged.addAll(reportsByIds);
		}

		if (personIds != null && !personIds.isEmpty()) {
			List<Report> reportsByPersons = allowChangeNonPending ? reportService.getByPersonIdIn(personIds) : reportService.getByPersonIdInAndStatus(personIds, ReportStatus.PENDING);
			log.info("Found {} reports by person Ids, marking for recalculation", reportsByPersons.size());
			toBeChanged.addAll(reportsByPersons);
		}

		if (personIds != null && !personIds.isEmpty()) {
			List<Report> reportsByPersons = allowChangeNonPending ? reportService.getByEmploymentIdIn(employmentIds) : reportService.getByEmploymentIdAndStatus(employmentIds, ReportStatus.PENDING);
			log.info("Found {} reports by Employment Ids, marking for recalculation", reportsByPersons.size());
			toBeChanged.addAll(reportsByPersons);
		}
		return toBeChanged;
	}

	public String getApiKey() throws NoSuchElementException {
		Optional<ApiAccess> first = apiAccessDao.findFirstByTypeAndDisabledFalse(ApiType.INTERNAL);
		if (first.isEmpty()) {
			ApiAccess apiAccess = new ApiAccess();
			apiAccess.setApiKey(UUID.randomUUID().toString());
			apiAccess.setType(ApiType.INTERNAL);
			apiAccess.setDisabled(false);
			apiAccessDao.save(apiAccess);
			first = apiAccessDao.findFirstByTypeAndDisabledFalse(ApiType.INTERNAL);
		}

		return first.orElseThrow().getApiKey();
	}

	public void resetStatistics() {
		statisticService.clearAll();
	}
}
