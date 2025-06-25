package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.controller.rest.DeadlineRestController;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.DeadlineEmailsService;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.AuditLogService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.cms.CmsMessageBundle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

@Slf4j
@Controller
@RequireAdministrator
@RequiredArgsConstructor
public class AdminController {

    private final AddressService addressService;
    private final CmsMessageBundle cmsMessageBundle;
    private final SubstituteService substituteService;
    private final OrgUnitService orgUnitService;
    private final DeadlineEmailsService deadlineEmailsService;
    private final ReportService reportService;
    private final SecurityUtil securityUtil;
    private final AuditLogService auditLogService;
    private final PersonService personService;

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("cmsMessage", cmsMessageBundle.getAll());
        model.addAttribute("personal", false);
        model.addAttribute("adminView", true);
        return "admin/admin";
    }

    @GetMapping("/admin/report/list")
    public String ouFragment(Model model, @RequestParam(name = "status") ReportStatus status) {
        reportService.populateAdminReportList(model, status);
        model.addAttribute("approve", true);
        model.addAttribute("adminView", true);
        model.addAttribute("orgUnits", orgUnitService.getAll());
        return "report/listFragment :: report-list";
    }

    @GetMapping("/admin/approvers")
    public String substituteApprover(Model model) {
        List<Substitute> allWithOrgunit = substituteService.findAllWithOrgUnit();
        List<Substitute> allWithoutOrgUnit = substituteService.findAllWithoutOrgUnit();
        List<OrgUnit> orgUnits = orgUnitService.getAll();

        Collections.sort(orgUnits, Comparator.comparing(OrgUnit::getLongDescription));

        model.addAttribute("substitutes", allWithOrgunit);
        model.addAttribute("personalSub", allWithoutOrgUnit);
        model.addAttribute("orgUnits", orgUnits);

        return "admin/substituteApproverFragment";
    }

    public void populateModel(Model model, boolean filterByPerson) {
        List<Substitute> allWithOrgunit = substituteService.findAllWithOrgUnit();
        List<Substitute> allWithoutOrgUnit = substituteService.findAllWithoutOrgUnit();
        List<OrgUnit> orgUnits = orgUnitService.getAll();


        if (filterByPerson) {
            Person loggedInPerson = securityUtil.getPerson();

            allWithOrgunit = allWithOrgunit.stream()
                    .filter(substitute -> Objects.equals(loggedInPerson, substitute.getSubstituteFor()))
                    .toList();

            allWithoutOrgUnit = allWithoutOrgUnit.stream()
                    .filter(substitute -> Objects.equals(loggedInPerson, substitute.getSubstituteFor()))
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
        }

        model.addAttribute("substitutes", allWithOrgunit);
        model.addAttribute("personalSub", allWithoutOrgUnit);
        model.addAttribute("orgUnits", orgUnits);
    }
    
    @GetMapping("/admin/deadlineFragment")
    public String deadlineFragment(Model model) {
        
        model.addAttribute("deadlines", deadlineEmailsService.getAll());
        model.addAttribute("defaultSubject", cmsMessageBundle.getText("cms.help.email.deadline.subject"));
        model.addAttribute("defaultMessage",  cmsMessageBundle.getText("cms.help.email.deadline.body"));
        model.addAttribute("deadlineRecord", new DeadlineRestController.DeadlineRecord(0L, "","", null, null, false));
        
        return "admin/deadlineFragment";
    }

    @GetMapping("admin/standardAddressFragment")
    public String standardAddressFragment(Model model) {
        model.addAttribute("addresses", addressService.getStandardAddresses());
        model.addAttribute("primaryAddress", addressService.getPrimary());
        return "admin/standardAddressFragment";
    }

    public record StandardAddressRec(long id, String description, double lat, double lon, String house_number, String road, String town, int postcode) {}

    @PostMapping("rest/standardAddress/create")
    public ResponseEntity<String> createStandardAddress(@RequestBody StandardAddressRec sAddr) {
        Address addr = new Address();
        addr.setStandardAddress(true);
        addr.setStreetName(sAddr.road);
        addr.setStreetNumber(sAddr.house_number);
        addr.setZipCode(sAddr.postcode);
        addr.setTown(sAddr.town);
        addr.setLatitude(sAddr.lat);
        addr.setLongitude(sAddr.lon);
        addr.setDescription(sAddr.description);
        addr.setType(AddressType.STANDARD);
        addressService.save(addr);
        
        Person loggedInPerson = securityUtil.getPerson();
        auditLogService.save(loggedInPerson.getId(), loggedInPerson.getName(), LogAction.CREATE_STANDARD_ADDRESS, "", addr);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("rest/standardAddress/delete/{id}")
    public ResponseEntity<String> deleteStandardAddress(@PathVariable long id) {
        Address addr = addressService.getById(id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!addr.isStandardAddress()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addressService.delete(addr.getId());
    
    
        Person loggedInPerson = securityUtil.getPerson();
        auditLogService.save(loggedInPerson.getId(), loggedInPerson.getName(), LogAction.DELETE_STANDARD_ADDRESS, "", addr);
    
        return ResponseEntity.ok().build();
    }

    @PostMapping("rest/standardAddress/edit")
    public ResponseEntity<String> updateStandardAddress(@RequestBody StandardAddressRec sAddr) {
        Address addr = addressService.getById(sAddr.id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!addr.isStandardAddress()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addr.setStreetName(sAddr.road);
        addr.setStreetNumber(sAddr.house_number);
        addr.setZipCode(sAddr.postcode);
        addr.setTown(sAddr.town);
        addr.setLatitude(sAddr.lat);
        addr.setLongitude(sAddr.lon);
        addr.setDescription(sAddr.description);
        addressService.save(addr);
    
    
        Person loggedInPerson = securityUtil.getPerson();
        auditLogService.save(loggedInPerson.getId(), loggedInPerson.getName(), LogAction.UPDATE_STANDARD_ADDRESS, "", addr);
    
        return ResponseEntity.ok().build();
    }

    @PostMapping("rest/standardAddress/prime/{id}")
    public ResponseEntity<String> primeAddress(@PathVariable long id) {
        Address addr = addressService.getById(id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (addr.isPrimary()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!addr.isStandardAddress()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addressService.makeAddressPrimary(addr);
    
        Person loggedInPerson = securityUtil.getPerson();
        auditLogService.save(loggedInPerson.getId(), loggedInPerson.getName(), LogAction.CHANGE_PRIME_ADDRESS, "", addr);
    
        return ResponseEntity.ok().build();
    }

    @GetMapping("admin/addressWashFragment")
    public String addressWashFragment(Model model) {
        model.addAttribute("unwashedAddresses", addressService.getDirtyAddresses());
        model.addAttribute("primaryAddress", addressService.getPrimary());
        return "admin/addressWashFragment";
    }

    @PostMapping("rest/waddress/{id}")
    public ResponseEntity<String> cleanDirtyAddress(@PathVariable long id) {
        Address addr = addressService.getById(id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!addr.isDirty()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addr.setDirtyString(addr.getAddressString());
        addressService.save(addr);
        return ResponseEntity.ok().build();
    }

    public record WashedAddress (long id, String streetName, String streetNumber, int zipCode, String town, double latitude, double longitude) {}
    @PostMapping("rest/waddress/")
    public ResponseEntity<String> editDirtyAddress(@RequestBody WashedAddress waddr) {
        Address addr = addressService.getById(waddr.id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!StringUtils.hasLength(addr.getDirtyString())) {
            addr.setDirtyString(addr.getAddressString());
        }
        addr.setStreetName(waddr.streetName);
        addr.setStreetNumber(waddr.streetNumber);
        addr.setZipCode(waddr.zipCode);
        addr.setTown(waddr.town);
        addr.setLatitude(waddr.latitude);
        addr.setLongitude(waddr.longitude);
        addressService.save(addr);
        return ResponseEntity.ok().build();
    }

    @PostMapping("rest/waddress/delete/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable long id) {
//        Address addr = addressService.getById(id);
//        if (addr == null) {
//            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
//        }
//        if (!addr.isDirty()) {
//            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
//        }
//        addressService.delete(addr.getId());
//        return ResponseEntity.ok().build();
        throw new NotImplementedException();
    }

    @PostMapping("rest/waddress/dirtify/{id}")
    public ResponseEntity<String> dirtifyAddress(@PathVariable long id) {
        Address addr = addressService.getById(id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (!addr.isDirty()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addr.setDirtyString(null);
        addr.setLatitude(0);
        addr.setLongitude(0);
        addressService.save(addr);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/overview")
    public String ouFragment(Model model) {
        model.addAttribute("admins", personService.getAllAdmins());
        return "admin/adminOverviewFragment";
    }
}
