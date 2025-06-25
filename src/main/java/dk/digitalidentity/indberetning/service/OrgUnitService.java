package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.OrgUnitDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUnitService {

	private final OrgUnitDao orgUnitDao;

	public List<OrgUnit> getAll() {
		return orgUnitDao.findAll();
	}
	public List<OrgUnit> saveAll(List<OrgUnit> toBeCreated) {
		return orgUnitDao.saveAll(toBeCreated);
	}

	public OrgUnit findById(Long id) {
		return orgUnitDao.findById(id).orElse(null);
	}

	public List<OrgUnit> findStartingWith(String prefix) {
		// Fetch all potential matches from the database
		List<OrgUnit> orgUnits = orgUnitDao.findOrgUnitByLongDescriptionContaining(prefix);

		// Rank and sort the results based on token-based matching
		Comparator<OrgUnit> comparator = Comparator.comparingDouble(orgUnit -> SearchUtil.calculateTokenMatchScore(orgUnit.getLongDescription(), prefix));
		return orgUnits.stream()
				.sorted(comparator.reversed()) // Sort by match score descending
				.collect(Collectors.toList());
	}

	public Person findLeaderOfOrgUnit(OrgUnit orgUnit) {
		if (orgUnit == null) {
			log.warn("Null OU");
			return null;
		}

		for (Employment employment : orgUnit.getEmployments()) {
			if (employment.isLeader() && employment.getStartDate().isBefore(LocalDateTime.now()) && (employment.getStopDate() == null || employment.getStopDate().isAfter(LocalDateTime.now()) || employment.getStopDate().equals(LocalDateTime.now()))) {
				return employment.getPerson();
			}
		}
		log.debug("The orgunit: {} does not have a leader!", orgUnit.getLongDescription());
		return null;
	}

	public record OuTreeLeaderResult(Person leader, OrgUnit leadersOU) {}
	public OuTreeLeaderResult findLeaderOfOrgUnitTree(OrgUnit orgUnit) {
		Person leader = findLeaderOfOrgUnit(orgUnit);
		if (leader != null) {
			return new OuTreeLeaderResult(leader, orgUnit);
		}

		if (orgUnit.getParent() != null) {
			return findLeaderOfOrgUnitTree(orgUnit.getParent());
		}

		log.warn("The OrgUnit does not have a leader, even when searching parent OUs");
		return null;
	}

	// This is date specific, so if you stopped in an OU as leader you will not have access to it.
	public List<OrgUnit> findOrgUnitsILead(Person person, LocalDate date) {
		List<OrgUnit> result = new ArrayList<>();
		for (Employment employment : person.getEmployments()) {
			if (employment.isLeader() && dateBetweenInclusive(date, employment.getStartDate(), employment.getStopDate())) {
				result.add(employment.getOrgUnit());
			}
		}
		return result;
	}

	private boolean dateBetweenInclusive(LocalDate date, LocalDateTime startDate, LocalDateTime endDate) {
		boolean inclusiveBegin = startDate == null || date.isEqual(startDate.toLocalDate()) || date.isAfter(startDate.toLocalDate());
		boolean inclusiveEnd =  endDate == null || date.isEqual(endDate.toLocalDate()) || date.isBefore(endDate.toLocalDate());

		return inclusiveBegin && inclusiveEnd;
	}

	public List<Person> findAllLeaders() {
		List<Person> result = new ArrayList<>();

		List<OrgUnit> orgUnit = getAll();
		for (OrgUnit unit : orgUnit) {
			if (orgUnit == null) {
				log.warn("No OrgUnits have been found!");
			}
			else {
				for (Employment employment : unit.getEmployments()) {
					if (employment.isLeader()) {
						result.add(employment.getPerson());
					}
				}
			}
		}
		return result;
	}

	// This code assumes you lead ALL employments in an OU, even if they have stopped. Giving you access to lookups and the like.
	public List<Employment> findAllEmploymentsILead(Set<OrgUnit> orgUnits, Set<Long> substituteExclusiveModeOUs, boolean ignoreExclusiveMode) {
		ArrayList<Employment> result = new ArrayList<>();
		if (orgUnits != null && !orgUnits.isEmpty()) {

			for (OrgUnit orgUnit : orgUnits) {
				List<Employment> leaders = orgUnit.getEmployments().stream().filter(Employment::isLeader).toList();
				if (leaders.isEmpty() && (ignoreExclusiveMode || !substituteExclusiveModeOUs.contains(orgUnit.getId()))) {
					result.addAll(orgUnit.getEmployments());
					result.addAll(findAllEmploymentsILead(orgUnit.getChildren(), substituteExclusiveModeOUs, ignoreExclusiveMode));
				}
				else {
					result.addAll(leaders);
				}
			}
		}
		return result;
	}

	public OrgUnit save(OrgUnit toBeCreated) {
		return orgUnitDao.save(toBeCreated);
	}
	public OrgUnit findByOrgId(String orgId) {
		return orgUnitDao.findByOrgId(orgId);
	}
}
