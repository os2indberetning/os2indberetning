package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.entity.ReportView;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class DatatableSpecBuilderUtil {
	@NotNull
	public static Specification<ReportView> getReportViewSpecification(ReportStatus status, LocalDate startDate, LocalDate endDate, Long personId, String orgUnitName) {
		//Default only search on Status
		Specification<ReportView> spec = ReportView.getByStatus(status);
		if (status.equals(ReportStatus.ACCEPTED)) {
			spec = spec.or(ReportView.getByStatus(ReportStatus.INVOICED));
		}

		if (status.equals(ReportStatus.REJECTED)) {
			spec = spec.or(ReportView.getByStatus(ReportStatus.REJECTED_AFTER_INVOICE));
		}

		if (personId != null) {
			spec = spec.and(ReportView.getById(personId));
		}

		if (StringUtils.hasLength(orgUnitName)) {
			spec = spec.and(ReportView.getByOrgUnit(orgUnitName));
		}

		if (endDate != null) {
			spec = spec.and(ReportView.isBeforeDate(endDate));
		}
		if (startDate != null) {
			spec = spec.and(ReportView.isAfterDate(startDate));
		}
		return spec;
	}
}
