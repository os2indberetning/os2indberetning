package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.AppLogin;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.PersonalRouteService;
import dk.digitalidentity.indberetning.service.AppLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class PersonalSettingsController {

    private final AddressService addressService;
    private final AppLoginService appLoginService;
    private final LicensePlateService licensePlateService;
    private final SecurityUtil securityUtil;
    private final PersonalRouteService personalRouteService;

    record EmailPreferences(boolean receivePersonalMail, boolean receiveApproverMail, boolean receiveAdminMail) { }
    @GetMapping("/settings")
    public String settings(Model model) {
        Person user = securityUtil.getPerson();
        List<LicensePlate> userPlates = licensePlateService.getByPersonId(user.getId());
        model.addAttribute("user",user);
        model.addAttribute("userPlates",userPlates);
        model.addAttribute("emailPreferences", new EmailPreferences(user.isReceivePersonalMail(), user.isReceiveApproverMail(), user.isReceiveAdminMail()));
        return "settings/settings";
    }

    @GetMapping("/settings/addressFragment")
    public String addressFragment(Model model) {
        Address officialHomeAddress = addressService.getOfficialHomeAddress(securityUtil.getPerson(), LocalDateTime.now());
        Address actualHomeAddress = addressService.getHomeAddressByDate(securityUtil.getPerson(), LocalDateTime.now());
        actualHomeAddress = officialHomeAddress == actualHomeAddress ? null : actualHomeAddress;
        
        model.addAttribute("homeAddress", officialHomeAddress);
        model.addAttribute("actualHomeAddress", actualHomeAddress);

        // Returns a set that includes both the calculated and official work addresses for each employment.
        model.addAttribute("workAddresses", addressService.getWorkByDate(securityUtil.getPerson(),LocalDateTime.now()));

        // Used to display all your adresses and allow editing of any non home/work/dhome/dwork addresses
        model.addAttribute("addresses",addressService.getAvailableAddressesByDate(securityUtil.getPerson(), LocalDateTime.now(), false));

        // Used for map initialization
        model.addAttribute("primaryAddress", addressService.getPrimary());
        return "settings/addressFragment";
    }

    @GetMapping("/settings/personalRoutesFragment")
    public String routesFragment(Model model) {
        Person currentUser = securityUtil.getPerson();
        model.addAttribute("aAddresses", addressService.getAvailableAddresses(currentUser));
        model.addAttribute("primaryAddress", addressService.getPrimary());
        model.addAttribute("personalRoutes", personalRouteService.findByPerson(currentUser));
        return "settings/personalRoutesFragment";
    }

    @GetMapping("/settings/appLoginFragment")
    public String appLoginFragment(Model model) {
        AppLogin appLog = appLoginService.getByPersonId(securityUtil.getPerson().getId());
        model.addAttribute("appLog", appLog);
        return "settings/appLoginFragment";
    }
}