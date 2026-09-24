package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.entity.AppLogin;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.AppLoginService;
import dk.digitalidentity.indberetning.service.AuditLogService;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.RateService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Hidden
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class AppUserAuthRestController {
    private final AppLoginService appLoginService;
    private final RateService rateService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
	private final EmploymentService employmentService;

    public record AuthUserRequest(String UserName, String Password) {}
    public record AuthUuidRequest(UUID GuId) {}

    //These records are have taken from the old apps ViewModels
    public record UserInfoRec(ProfileRec profile, List<RateRec> rates) {}
    public record ProfileRec(long Id, String Firstname, String Lastname, String HomeLatitude, String HomeLongitude, AuthUuidRequest Authorization, Collection<EmploymentRec> Employments) {}
    public record RateRec(long Id, String Description, String Year) {}
    public record EmploymentRec(long Id, String EmploymentPosition, String ManNr, Long StartDateTimestamp, Long EndDateTimestamp, OrgUnitRec OrgUnit) {}
    public record OrgUnitRec(long OrgId, boolean FourKmRuleAllowed) {}

    @PostMapping("/appapi/auth")
    public ResponseEntity<UserInfoRec> auth(@RequestBody AuthUserRequest aReq) {
        AppLogin appLogin = appLoginService.getByUsername(aReq.UserName);
        if (appLogin == null) {
			log.warn("The app-user with name: {} does not exists", aReq.UserName);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!passwordEncoder.matches(aReq.Password, appLogin.getPassword())) {
			log.warn("The app-user with name: {} and id: {} does not match the right password", aReq.UserName, appLogin.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(generateUserInfo(appLogin));
    }

    @PostMapping("/appapi/userinfo")
    public ResponseEntity<UserInfoRec> userInfo(@RequestBody AuthUuidRequest uReq) {
        AppLogin appLogin = appLoginService.getByUuid(uReq.GuId.toString());
        if (appLogin == null || uReq.GuId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(generateUserInfo(appLogin));
    }

    private UserInfoRec generateUserInfo(AppLogin appLogin) {
        Person person = appLogin.getPerson();
        List<Employment> employments = employmentService.getEmploymentsByPersonAndDriveDate(person, LocalDate.now());
        List<EmploymentRec> employmentRecs = employments.stream()
			.map(employment -> new EmploymentRec(
							employment.getId(), 
							employment.getPosition(), 
							employment.getEmployeeNumber(), 
							ldtToLong(employment.getStartDate()), 
							ldtToLong(employment.getStopDate()), 
							new OrgUnitRec(
								employment.getOrgUnit().getId(), 
								employment.getOrgUnit().getFourKmRuleAllowed() != null && employment.getOrgUnit().getFourKmRuleAllowed()
							)
						))
			.toList();
        if (appLogin.getUuid() == null) {
            appLoginService.setNewUuid(appLogin);
        }

        ProfileRec profile = new ProfileRec(person.getId(), person.getFirstName(), person.getLastName(), "0", "0", new AuthUuidRequest(UUID.fromString(appLogin.getUuid())), employmentRecs);
        List<Rate> ratesProper = rateService.getByActiveYear(Year.now().getValue());
        List<RateRec> rateRecs = ratesProper.stream().map(rate -> new RateRec(rate.getId(), rate.getRateType().getName(), Integer.toString(rate.getActiveYear()))).toList();
        return new UserInfoRec(profile, rateRecs);
    }

    private Long ldtToLong(@Nullable LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }

        ZonedDateTime zonedLDT = ldt.atZone(ZoneId.of("Europe/Copenhagen"));
        return zonedLDT.toEpochSecond();
    }
}
