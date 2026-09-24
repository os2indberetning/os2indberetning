package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.RequireApprover;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequireApprover
@RequiredArgsConstructor
public class ApproveController {

	private final ReportService reportService;
	private final OrgUnitService orgUnitService;
	private final SubstituteService substituteService;
	private final SecurityUtil securityUtil;
	private final EmploymentService employmentService;

	@GetMapping("/approve")
	public String approve(Model model) {
		model.addAttribute("personal", false);
		model.addAttribute("adminView", true);
		return "approve/approve";
	}

	@GetMapping("/approve/substitutes")
	public String sApprover(Model model) {
		List<Substitute> allWithOrgunit = substituteService.findAllWithOrgUnit();
		List<Substitute> allWithoutOrgUnit = substituteService.findAllWithoutOrgUnit();
		List<OrgUnit> orgUnits = orgUnitService.getAll();


		Person loggedInPerson = securityUtil.getPerson();


		Set<Employment> employmentSet = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(loggedInPerson, LocalDate.now()));
		Set<Long> peopleUserLeads = employmentSet.stream().map(Employment::getPerson).map(Person::getId).collect(Collectors.toSet());

		allWithOrgunit = allWithOrgunit.stream()
				.filter(substitute -> Objects.equals(loggedInPerson.getId(), substitute.getSubstituteFor().getId()) || Objects.equals(loggedInPerson.getId(), substitute.getSubstitute().getId()))
				.toList();

		allWithoutOrgUnit = allWithoutOrgUnit.stream()
				.filter(substitute -> Objects.equals(loggedInPerson.getId(), substitute.getSubstituteFor().getId()) ||
								      Objects.equals(loggedInPerson.getId(), substitute.getSubstitute().getId()) ||
								      peopleUserLeads.contains(substitute.getSubstituteFor().getId())) // In addition to the persons own personal approver mappings they also get any of their employees mappings
				.toList();

		List<Long> oUIds = loggedInPerson.getEmployments().stream()
				.filter(Employment::isLeader)
				.map(employment -> employment.getOrgUnit().getId())
				.toList();

		orgUnits = orgUnits.stream()
				.filter(orgUnit -> oUIds.contains(orgUnit.getId()))
				.toList();

		model.addAttribute("approver", loggedInPerson);
		model.addAttribute("noLeadershipPositions", oUIds.isEmpty());

		model.addAttribute("substitutes", allWithOrgunit);
		model.addAttribute("personalSub", allWithoutOrgUnit);
		model.addAttribute("orgUnits", orgUnits);

		return "admin/substituteApproverFragment";
	}


	@GetMapping("/approve/report/list")
	public String ouFragment(Model model, @RequestParam(name = "status") ReportStatus status) {
		reportService.populateApproveReportList(model, status);
		model.addAttribute("orgUnits", orgUnitService.getAll());
		model.addAttribute("approve", true);
		model.addAttribute("adminView", true);
		model.addAttribute("personal", false);
		return "report/listFragment :: report-list";
	}


}
