package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
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

    private final AddressService addressService;
    private final OrgUnitService orgUnitService;

    @GetMapping("/admin/ouFragment")
    public String ouFragment(Model model) {
        List<OrgUnit> orgUnits = orgUnitService.getAll();
        model.addAttribute("orgUnits", orgUnits);
        return "admin/ouFragment";
    }

    public record OrgUnitInput(String orgId, boolean kmRule, CalculationType calculationType, String addressRoad, String addressNumber, int addressZip, String addressTown, double addressLatitude, double addressLongitude) {}
    @PostMapping("/admin/orgunit-edit/{id}") // TODO: BUG we dont use ID?
    public String saveOrgUnit(Model model, @RequestBody OrgUnitInput body) {
        OrgUnit orgUnit = orgUnitService.findByOrgId(body.orgId);
        if (orgUnit == null) {
            return "redirect:/admin";
        }

        orgUnit.setFourKmRuleAllowed(body.kmRule);
        orgUnit.setDefaultCalculationType(body.calculationType);

        orgUnitService.save(orgUnit);

//        Address addr = new Address();
//        if (orgUnit.getAddress() != null) {
//            addr = orgUnit.getAddress();
//        }
//        addr.setStreetName(body.addressRoad);
//        addr.setStreetNumber(body.addressNumber);
//        addr.setZipCode(body.addressZip);
//        addr.setTown(body.addressTown);
//        addr.setLatitude(body.addressLatitude);
//        addr.setLongitude(body.addressLongitude);
//        addr.setDescription("Primære adresse for " + orgUnit.getLongDescription());
//        addr.setType(AddressType.WORK);
//        addr.setOrgUnit(orgUnit);
//
//        addressService.save(addr);
//        orgUnit.setAddress(addr);
//        orgUnitService.save(orgUnit);

        return "redirect:/admin";
    }
}
