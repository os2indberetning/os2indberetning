package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import dk.digitalidentity.indberetning.security.RequireAdministratorOrApprover;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequireAdministratorOrApprover
public class SubstituteController {
    private final PersonService personService;
    private final OrgUnitService orgUnitService;
    private final SubstituteService substituteService;
    private final SecurityUtil securityUtil;
    private final String SUBSTITUTE_APPROVER_FRAGMENT = "admin/substituteApproverFragment";
    private final EmploymentService employmentService;

    public record SubstituteDTO(Long id, Long subId, Long subForId, Long orgUnitId, LocalDate startDate, LocalDate endDate,
                                Boolean unlimitedEndTime, Boolean substituteExclusiveMode) {}

    @PostMapping("/substitute/createPersonal")
    public String createPersonalApprover(@RequestBody SubstituteDTO substituteDTO) {
        Substitute sub = new Substitute();
        sub.setCreatedBy(securityUtil.getPerson());
        sub.setStartDate(substituteDTO.startDate);
        if (!Objects.equals(substituteDTO.subId, substituteDTO.subForId)) {
            sub.setSubstitute(personService.getById(substituteDTO.subId));
            sub.setSubstituteFor(personService.getById(substituteDTO.subForId));
        }
        if (!substituteDTO.unlimitedEndTime) {
            sub.setEndDate(substituteDTO.endDate);
        }
        substituteService.save(sub);

        return SUBSTITUTE_APPROVER_FRAGMENT;
    }

    @PostMapping("/substitute/editPersonalSub")
    public String editPersonalSub(@RequestBody SubstituteDTO substituteDTO) {
        Substitute sub = substituteService.findById(substituteDTO.id);
        if (sub == null) {
            log.warn("The substitute with id {} was not found.", substituteDTO.id);
            return SUBSTITUTE_APPROVER_FRAGMENT;
        }
        sub.setCreatedBy(securityUtil.getPerson());
        sub.setSubstitute(personService.getById(substituteDTO.subId));
        sub.setSubstituteFor(personService.getById(substituteDTO.subForId));
        if (substituteDTO.startDate == null) {
            log.warn("The start date cannot be null!");
            return SUBSTITUTE_APPROVER_FRAGMENT;
        }
        else {
            sub.setStartDate(substituteDTO.startDate);
        }

        if (!substituteDTO.unlimitedEndTime) {
            sub.setEndDate(substituteDTO.endDate);
        }

        substituteService.save(sub);

        return SUBSTITUTE_APPROVER_FRAGMENT;
    }

    @GetMapping("/substitute/getLeaderForPerson")
    public ResponseEntity<?> getLeaderForPerson(@RequestParam("personId") String personId) {
        Person person = personService.getById(Long.parseLong(personId));
        List<Long> listOfIds = new ArrayList<>();
        if (person != null) {

            List<OrgUnit> result = orgUnitService.findOrgUnitsILead(person, LocalDate.now());
            if (result != null) {
                for (OrgUnit orgUnit : result) {
                    listOfIds.add(orgUnit.getId());
                }
            }
        }
        return new ResponseEntity<>(listOfIds, HttpStatus.OK);
    }

    record PersonResult(Long id, String text) {}
    @GetMapping("/substitute/getLeaderForOrgUnit")
    public ResponseEntity<?> getLeaderForOrgUnits(@RequestParam("id") String id) {
        OrgUnit orgUnit = orgUnitService.findById(Long.parseLong(id));
        if (orgUnit == null) {
            log.warn("The provided id: " + id + " does not exist");
            return ResponseEntity.badRequest().build();
        }

        for (Employment employment : orgUnit.getEmployments()) {
            if (employment.isLeader()) {
                return new ResponseEntity<>(new PersonResult(employment.getPerson().getId(), employment.getPerson().getName()), HttpStatus.OK);
            }
        }

        log.warn("The organisation: " + orgUnit.getLongDescription() + " does not have a leader!");
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/substitute/createSub")
    public String createSubApprover(@RequestBody SubstituteDTO substituteDTO) {

        Substitute sub = new Substitute();

        if (substituteDTO.startDate == null) {
            log.warn("The start date cannot be null!");
        }
        else {
            sub.setStartDate(substituteDTO.startDate);
        }
        if (!substituteDTO.unlimitedEndTime) {
            sub.setEndDate(substituteDTO.endDate);
        }

        sub.setOrgUnit(orgUnitService.findById(substituteDTO.orgUnitId));
        sub.setSubstitute(personService.getById(substituteDTO.subId));
        sub.setSubstituteFor(personService.getById(substituteDTO.subForId));
        sub.setSubstituteExclusiveMode(substituteDTO.substituteExclusiveMode);
        sub.setCreatedBy(securityUtil.getPerson());

        substituteService.save(sub);

        return SUBSTITUTE_APPROVER_FRAGMENT;
    }

    @PostMapping("/substitute/editSub")
    public String editSubApprover(@RequestBody SubstituteDTO substituteDTO) {
        Substitute sub = substituteService.findById(substituteDTO.id);
        if (sub != null) {
            sub.setSubstitute(personService.getById(substituteDTO.subId));

            if (substituteDTO.startDate == null) {
                log.warn("The start date cannot be null!");
            }
            else {
                sub.setStartDate(substituteDTO.startDate);
            }

            if (substituteDTO.unlimitedEndTime) {
                sub.setEndDate(null);
            }
            else {
                sub.setEndDate(substituteDTO.endDate);
            }
            sub.setOrgUnit(orgUnitService.findById(substituteDTO.orgUnitId));
            sub.setCreatedBy(securityUtil.getPerson());
            sub.setSubstituteFor(personService.getById(substituteDTO.subForId()));
            sub.setSubstituteExclusiveMode(substituteDTO.substituteExclusiveMode);
            substituteService.save(sub);
        }
        else {
            log.warn("Substitute with id {} not found!", substituteDTO.id);
        }
        return SUBSTITUTE_APPROVER_FRAGMENT;
    }

    record Select2Result(long id, String text) {}
    record Select2Results(List<Select2Result> results) {}

    @GetMapping("/substitute/findByPrefix")
    public ResponseEntity<?> findByPrefix(@RequestParam("q") String prefix) {
        List<Person> persons = personService.findStartingWith(prefix);
        List<Select2Result> results = new ArrayList<>();

        for (Person person : persons) {
            results.add(new Select2Result(person.getId(), person.getName()));
        }

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    @GetMapping("/substitute/personal/subfor/findByPrefix")
    public ResponseEntity<?> findSubForByPrefix(@RequestParam("q") String prefix) {
        List<Person> persons = personService.findStartingWith(prefix);
        List<Select2Result> results = new ArrayList<>();

        // If it is an approver that calls this endpoint
        if (!securityUtil.isAdmin()) {
            Person loggedInPerson = securityUtil.getPerson();
            Map<Long, Person> collect = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(loggedInPerson, LocalDate.now())).stream()
                    .map(Employment::getPerson)
                    .collect(Collectors.toMap(Person::getId, Function.identity(), ((person, person2) -> person)));

            persons = persons.stream()
                    .filter(person -> collect.containsKey(person.getId()))
                    .collect(Collectors.toList());
        }

        for (Person person : persons) {
            results.add(new Select2Result(person.getId(), person.getName()));
        }

        return new ResponseEntity<>(new Select2Results(results), HttpStatus.OK);
    }

    @PostMapping("/substitute/deleteSub")
    public String deleteSubApprover(@RequestBody SubstituteDTO substituteDTO) {
        Substitute sub = substituteService.findById(substituteDTO.id);

        if (sub != null) {
            substituteService.delete(sub);
        }
        else {
            log.warn("The sub with id " + substituteDTO.id + " does not exist");
        }

        return SUBSTITUTE_APPROVER_FRAGMENT;
    }

    @GetMapping("/substitute/getAllLeaders")
    public ResponseEntity<?> getAllLeaders(@RequestParam("q") String prefix) {
        Set<Person> leaders = orgUnitService.findAllValidLeaders();
        List<Select2Result> result = new ArrayList<>();

        for (Person leader : leaders) {
            if (leader.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(new Select2Result(leader.getId(), leader.getName()));
            }
        }
        return new ResponseEntity<>(new Select2Results(result), HttpStatus.OK);
    }

    @GetMapping("/substitute/getPerson")
    public ResponseEntity<?> getPerson(@RequestParam("id") String id) {
        Person person = personService.getById(Long.parseLong(id));

        if (person != null ) {
            return new ResponseEntity<>(new Select2Result(person.getId(), person.getName()), HttpStatus.OK);
        }
        else {
            log.warn("The person with id: {} does not exist!", id);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
