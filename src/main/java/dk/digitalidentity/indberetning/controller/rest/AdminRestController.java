package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.datatable.dao.ReportDatatableDao;
import dk.digitalidentity.indberetning.model.entity.*;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@Hidden
@RestController
@RequireAdministrator
@RequiredArgsConstructor
public class AdminRestController {
    private final ReportDatatableDao reportDatatableDao;
    private final OrgUnitService orgUnitService;
    private final PersonService personService;
    private final SecurityUtil securityUtil;
    private final AuditLogService auditLogService;

    @PostMapping("/rest/admin/report/list")
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
                ; // ignored
            }
        }
        String orgUnitName = Objects.equals(null, orgUnit) ? "" : orgUnit.getLongDescription();

        Specification<ReportView> spec = DatatableSpecBuilderUtil.getReportViewSpecification(status, startDate, endDate, personId, orgUnitName);
        return reportDatatableDao.findAll(input, spec);
    }

    record emailAuditLogDetail(Long personId, String name, String adminEmailPreference) {}
    @PostMapping("/rest/admin/updateEmailNotification/{personId}")
    public ResponseEntity<String> updatePersonEmailFlag(@PathVariable("personId") String personId) {
        Person person = personService.getById(Long.parseLong(personId));
        if (person == null) {
            return ResponseEntity.badRequest().build();
        }
        person.setReceiveAdminMail(!person.isReceiveAdminMail());
        personService.save(person);

        // Log the update
        Person loggedInPerson = securityUtil.getPerson();

        emailAuditLogDetail emailAuditLogDetail = new emailAuditLogDetail(person.getId(), person.getName(), person.isReceiveAdminMail() ? "Modtager admin emails" : "Modtager ikke admin emails");
        auditLogService.save(loggedInPerson.getId(), loggedInPerson.getName(), LogAction.UPDATE_ADMIN_EMAIL_FLAG, "Preference for adressevask emails opdateret for: " + person.getName(), emailAuditLogDetail);

        return ResponseEntity.ok().build();
    }
}
