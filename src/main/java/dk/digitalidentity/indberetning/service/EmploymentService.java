package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.dao.EmploymentDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmploymentService {

	private final EmploymentDao employmentDao;
	private final OS2indberetningConfiguration configuration;

	public List<Employment> save(List<Employment> employmentsToBeSaved) {
		return employmentDao.saveAll(employmentsToBeSaved);
	}

	public Employment getById(long id) {
		return employmentDao.findById(id).orElse(null);
	}

	public Set<Employment> getByIdIn(Set<Long> ids) {
		return employmentDao.findByIdIn(ids);
	}

	public List<Employment> getByPerson(Person person) {
		return employmentDao.findByPerson(person);
	}

	public List<Employment> getByPersonButNotAfter14Days(Person person) {
		List<Employment> byPerson = getByPerson(person);
		List<Employment> result = new ArrayList<>();
		for (Employment employment : byPerson) {
			if (employment.getStopDate() == null || employment.getStopDate().plusDays(configuration.getEmploymentCloseDelay()).isAfter(LocalDateTime.now())) {
				result.add(employment);
			}
		}
		return result;
	}
	
	public List<Employment> getByPersonAndOrgUnit(Person person, OrgUnit orgUnit) {
		return employmentDao.findByPersonAndOrgUnit(person, orgUnit);
	}

	public List<Employment> findByStopDateBeforeOrStopDate(LocalDateTime date) {
		return employmentDao.findByStopDateBeforeOrStopDate(date, date);
	}

	public List<Employment> findByStopDateAfterOrStopDateNull(LocalDateTime date) {
		return employmentDao.findByStopDateAfterOrStopDateNull(date);

	}
}
