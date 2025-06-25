package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.SubstituteDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubstituteService {

    private final SubstituteDao substituteDao;
    private final OrgUnitService orgUnitService;
    private final EmploymentService employmentService;

    @Getter
    @Setter
    private boolean changed = false;

    @CacheEvict(value = "WhoDoILeadOrSub", allEntries=true)
    public void clearCache() {
        log.debug("WhoDoILeadOrSub Cache cleared");
    }

    @Cacheable(value = "WhoDoILeadOrSub", key = "{#person.id, #date}")
    public Set<Long> whoDoILeadOrSub(Person person, LocalDate date) {
        Set<Employment> employments = whoDoILeadOrSubNotCached(person, date);

        // We only return (and therefore cache) the IDs since calling on Hibernate Objects in a cache leads to all kinds of stuff
        return employments.stream().map(Employment::getId).collect(Collectors.toSet());
    }

    public Set<Employment> whoDoILeadOrSubNotCached(Person person, LocalDate date) {
        StopWatch stopWatch = null;
		if (log.isDebugEnabled()) {
            stopWatch = new StopWatch();
            stopWatch.start();
        }
        Set<Employment> result = new HashSet<>();

        // Add all employments that the leader I substitute is in charge of.
        addEmploymentsFromLeaderISub(person, date, result);

        // Add all employments of any person where I have been selected as the personal approver.
        addEmploymentsWherePersonIsPersonalApprover(person, date, result);

        // Setup for recursive call through OU tree, to minimize database calls.
        addEmploymentsFromMyLeadershipPositions(person, date, result);

        // Filters people with personal approvers,
        // and makes sure you do not lead or sub yourself.
        result = filterResults(person, result, date);

        if (log.isDebugEnabled()) {
            stopWatch.stop();
            log.debug("whoDoILeadOrSub found a total of {} employments for person {}", result.size(), person.getName());
            log.debug("whoDoILeadOrSub calculation took: " + stopWatch.getTotalTimeMillis() + "ms");
            log.debug("whoDoILeadOrSub employeeNumbers: " + result.stream().map(Employment::getEmployeeNumber).toList());
        }

        // Any other unhandled case can be fixed by assigning personal approvers
        return result;
    }

    private void addEmploymentsFromMyLeadershipPositions(Person person, LocalDate date, Set<Employment> result) {
        Set<Substitute> exclusiveSubstitutes = substituteDao.findBySubstituteExclusiveModeTrueAndSubstituteForAndOrgUnitNotNull(person);
        Set<Long> ousWithExclusiveSubstitutes = exclusiveSubstitutes.stream()
                .filter(substitute -> DateUtil.dateBetweenInclusive(date, substitute.getStartDate(), substitute.getEndDate()))
                .map(Substitute::getOrgUnit).map(OrgUnit::getId).collect(Collectors.toSet());
        HashSet<OrgUnit> orgUnits = new HashSet<>(orgUnitService.findOrgUnitsILead(person, date));

        for (OrgUnit ou : orgUnits) {
            if (!ousWithExclusiveSubstitutes.contains(ou.getId())) {
                result.addAll(
                        ou.getEmployments().stream()
                                .filter(employment -> !employment.isLeader())
                                .toList());
            }
            result.addAll(orgUnitService.findAllEmploymentsILead(ou.getChildren(), ousWithExclusiveSubstitutes, false));
        }
    }

    private void addEmploymentsWherePersonIsPersonalApprover(Person person, LocalDate date, Set<Employment> result) {
        for (Substitute sub : findAllWithoutOrgUnitByPerson(person)) {
            if (DateUtil.dateBetweenInclusive(date, sub.getStartDate(), sub.getEndDate())) {
                result.addAll(sub.getSubstituteFor().getEmployments());
            }
        }
    }

    private void addEmploymentsFromLeaderISub(Person person, LocalDate date, Set<Employment> result) {
        List<Substitute> mySubstitutePositions = substituteDao.findBySubstitute(person).stream()
                .filter(substitute -> DateUtil.dateBetweenInclusive(date, substitute.getStartDate(), substitute.getEndDate()))
                .collect(Collectors.toList());

        for (Substitute mySubstitutePosition : mySubstitutePositions) {
            OrgUnit ou = mySubstitutePosition.getOrgUnit();
            if (ou != null) {
                result.addAll(
                        ou.getEmployments().stream()
                                .filter(employment -> !employment.isLeader())
                                .toList());
                result.addAll(orgUnitService.findAllEmploymentsILead(ou.getChildren(), new HashSet<>(), true));
            }
        }
    }

    @NotNull
    private Set<Employment> filterResults(Person person, Set<Employment> result, LocalDate date) {
        // Get a map of all the persons with a personal approver, the value is a list of the personal approvers.
        List<Substitute> allPersonalApprovers = findAllWithoutOrgUnit();
        Map<Person, List<Person>> peopleWithPersonalApprovers = new HashMap<>();
        for (Substitute allPersonalApprover : allPersonalApprovers) {
            if (DateUtil.dateBetweenInclusive(date, allPersonalApprover.getStartDate(), allPersonalApprover.getEndDate())) {
                if (!peopleWithPersonalApprovers.containsKey(allPersonalApprover.getSubstituteFor())) {
                    peopleWithPersonalApprovers.put(allPersonalApprover.getSubstituteFor(), new ArrayList<>());
                }
                peopleWithPersonalApprovers.get(allPersonalApprover.getSubstituteFor()).add(allPersonalApprover.getSubstitute());
            }
        }

        // Filter people with personal approvers that are not the supplied person.
        result = result.stream().filter(employment -> {
            return !peopleWithPersonalApprovers.containsKey(employment.getPerson()) || peopleWithPersonalApprovers.get(employment.getPerson()).contains(person);
        }).collect(Collectors.toSet());

        // Remove all of my own employments since you can't lead yourself
        // This is both a sanity check and to fix the case where a leader also has a second employment in the same OU
        for (Employment employment : person.getEmployments()) {
			result.remove(employment);
        }
        return result;
    }

    public List<Substitute> findAll() {
        return substituteDao.findAll();
    }
    public Substitute findById(Long id) {
        return substituteDao.findById(id).orElse(null);
    }

    public Substitute save(Substitute substitute) {
        this.changed = true;
        return substituteDao.save(substitute);
    }
    public List<Substitute> findByOrgUnit(OrgUnit orgUnit) {
        return substituteDao.findByOrgUnit(orgUnit);
    }
    public List<Substitute> findAllWithOrgUnit() {
        return substituteDao.findByOrgUnitNotNull();
    }

    public List<Substitute> findAllWithoutOrgUnit() {
        return substituteDao.findByOrgUnitNull();
    }

    public List<Substitute> findAllWithoutOrgUnitByPerson(Person substitute) {
        return substituteDao.findByOrgUnitNullAndSubstitute(substitute);
    }

    public List<Substitute> saveAll(List<Substitute> substitutes) {
        this.changed = true;
        return substituteDao.saveAll(substitutes);
    }
    public List<Substitute> findWhoSubsPerson(Person person, OrgUnit orgUnit, LocalDate fromDate) {
        return substituteDao.findBySubstituteForAndOrgUnitAndStartDateLessThanEqual(person, orgUnit, fromDate);
    }

    public void delete(Substitute sub) {
        this.changed = true;
        substituteDao.deleteById(sub.getId());
    }

    public List<Substitute> findWhoPersonallyApprovesPerson(Person person, LocalDate fromDate) {
        return substituteDao.findBySubstituteForAndOrgUnitNullAndStartDateLessThanEqual(person, fromDate);
    }

    public List<Substitute> findByPerson(Person person) {
        return substituteDao.findBySubstituteOrSubstituteFor(person, person);
    }

    @Transactional
    public void checkForTerminations() {
        List<Person> inActive = employmentService.findByStopDateBeforeOrStopDate(LocalDateTime.now()).stream().map(Employment::getPerson).collect(Collectors.toCollection(ArrayList::new));
        List<Substitute> terminated = new ArrayList<>();

        for (Person person : inActive) {
            List<Substitute> subAssignments = findByPerson(person);
            if (subAssignments == null || subAssignments.isEmpty()) {
                continue;
            }

            subAssignments.forEach(subAssignment -> {
                // The idea is to look at the person that is "expired" and check if they have started a new employment at the same OU, if they did the assignment is still valid
                List<OrgUnit> activeEmployments = person.getEmployments().stream()
                        .filter(employment -> employment.getStopDate() == null || employment.getStopDate().isAfter(LocalDateTime.now()))
                        .map(employment -> employment.getOrgUnit()).collect(Collectors.toList());

                if ((activeEmployments == null || activeEmployments.isEmpty()) || !activeEmployments.contains(subAssignment.getOrgUnit())) {
                    if (!subAssignment.isFinished()) {
                        subAssignment.setFinished(true);
                        terminated.add(subAssignment);
                    }
                }
            });
        }
        log.debug("Applied terminated flag to {} assignments", terminated.size());
        saveAll(terminated);
    }
}
