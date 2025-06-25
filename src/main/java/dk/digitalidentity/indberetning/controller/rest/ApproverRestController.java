package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.datatable.dao.ReportDatatableDao;
import dk.digitalidentity.indberetning.model.entity.*;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.RequireApprover;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@Hidden
@RestController
@RequireApprover
@RequiredArgsConstructor
public class ApproverRestController {
	private final ReportDatatableDao reportDatatableDao;
	private final SubstituteService substituteService;
	private final OrgUnitService orgUnitService;
	private final SecurityUtil securityUtil;
	private final EmploymentService employmentService;

	@PostMapping("/rest/approve/report/list")
	public DataTablesOutput<ReportView> paginatingTable(@RequestBody DataTablesInput input, @RequestParam(name = "status") ReportStatus status, @RequestParam(name = "startDate", required = false) LocalDate startDate,
														@RequestParam(name = "endDate", required = false) LocalDate endDate,
														@RequestParam(name = "employeeSearch", required = false) Long personId,
														@RequestParam(name = "orgUnitSearch", required = false) String orgUnitId) {
		OrgUnit orgUnit = null;
		if(orgUnitId != null) {
			try {
				orgUnit = orgUnitService.findById(Long.parseLong(orgUnitId));
			}
			catch (NumberFormatException ignored) {
				;
			}
		}
		String orgUnitName = Objects.equals(null, orgUnit) ? "" : orgUnit.getLongDescription();

		Specification<ReportView> spec = DatatableSpecBuilderUtil.getReportViewSpecification(status, startDate, endDate, personId, orgUnitName);

		// Filter result set to not include any employment you are not the approver of.
		List<String> employeeNumbers = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now())).stream()
				.map(Employment::getEmployeeNumber)
				.toList();
		Specification<ReportView> approverFilter = (root, query, criteriaBuilder) -> root.get("employeeNumber").in(employeeNumbers);
		spec = spec.and(approverFilter);
		return reportDatatableDao.findAll(input, spec);
	}
}
