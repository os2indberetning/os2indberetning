package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.RateTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Slf4j
@Controller
@RequireAdministrator
@RequiredArgsConstructor
public class OrgUnitController {

    private final OrgUnitService orgUnitService;
	private final RateTypeService rateTypeService;

	@GetMapping("/admin/ouFragment")
    public String ouFragment(Model model) {
        List<OrgUnit> orgUnits = orgUnitService.getAll();
        model.addAttribute("orgUnits", orgUnits);
        model.addAttribute("rateTypes", rateTypeService.getAll());
        return "admin/ouFragment";
    }

    public record OrgUnitInput(String orgId, boolean kmRule, CalculationType calculationType, String addressRoad, String addressNumber, int addressZip, String addressTown, double addressLatitude, double addressLongitude, Long rateTypeId) {}
    @PostMapping("/admin/orgunit-edit")
    public String saveOrgUnit(Model model, @RequestBody OrgUnitInput body) {
        OrgUnit orgUnit = orgUnitService.findByOrgId(body.orgId);
        if (orgUnit == null) {
            return "redirect:/admin";
        }

		if (body.rateTypeId != null) {
			RateType rateType = rateTypeService.getById(body.rateTypeId);
			if (rateType == null) {
				log.warn("RateType with id {} did not exist!", body.rateTypeId);
				return "redirect:/admin";
			}
			orgUnit.setDefaultRateType(rateType);
		} else {
			orgUnit.setDefaultRateType(null);
		}

		orgUnit.setFourKmRuleAllowed(body.kmRule);
		orgUnit.setDefaultCalculationType(body.calculationType);

		orgUnitService.save(orgUnit);

        return "redirect:/admin";
    }
}
