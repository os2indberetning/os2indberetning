package dk.digitalidentity.indberetning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.dao.EmploymentDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmploymentService {
	private final EmploymentDao employmentDao;
	private final OS2indberetningConfiguration configuration;

	public List<Employment> save(List<Employment> employmentsToBeSaved) {
		return employmentDao.saveAll(employmentsToBeSaved);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public List<Employment> saveAllInIsolatedTransaction(List<Employment> employmentsToBeSaved) {
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

	public List<Employment> getByPersonButNotAfterCloseDelay(final Person person) {
		List<Employment> employments = getByPerson(person).stream()
			.filter(employment -> employment.getStopDate() == null 
					|| employment.getStopDate().plusDays(configuration.getEmploymentCloseDelay()).isAfter(LocalDateTime.now()))
			.collect(Collectors.toList());

		if(!configuration.isShowExcludedOrgUnitsInUi()) {
			employments = employments.stream()
				.filter(employment -> employment.getOrgUnit() != null && !employment.getOrgUnit().isExcludeFromMaxDistanceToSubtract())
				.collect(Collectors.toList());
		}

		return employments;
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

    public List<Employment> findByStopDateBefore(LocalDateTime stopDate) {
		return employmentDao.findByStopDateBefore(stopDate);
    }

	public List<Employment> getEmploymentsByPersonAndDriveDate(final Person person, final LocalDate driveDate) {
        List<Employment> employments = getByPerson(person) 
				.stream()
                .filter(Objects::nonNull)
                .filter(employment -> employment.getStartDate() == null || employment.getStartDate().minusDays(1).isBefore(driveDate.atStartOfDay()))
                .filter(employment -> employment.getStopDate() == null || employment.getStopDate().plusDays(1).isAfter(driveDate.atStartOfDay()))
                .collect(Collectors.toList());
		
		if(!configuration.isShowExcludedOrgUnitsInUi()) {
			employments = employments.stream()
				.filter(employment -> employment.getOrgUnit() != null && !employment.getOrgUnit().isExcludeFromMaxDistanceToSubtract())
				.collect(Collectors.toList());
		}

		return employments;
	}

	public void deleteAll(List<Employment> employments) {
		employmentDao.deleteAll(employments);
	}

	public void deleteAllInAud(List<Employment> employments) {
		ArrayList<Long> ids = employments.stream().map(Employment::getId).collect(Collectors.toCollection(ArrayList::new));
			employmentDao.deleteInAud(ids);
	}

	public List<Employment> findAll() {
		return employmentDao.findAll();
	}
}
