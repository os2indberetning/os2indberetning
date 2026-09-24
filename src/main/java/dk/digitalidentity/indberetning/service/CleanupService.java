package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupService {

	private final ReportService reportService;
	private final EmploymentService employmentService;
	private final PersonService personService;

	@Transactional(rollbackFor = Exception.class)
	public void cleanup(boolean dryRun) {
		log.debug("Running task: checkForExpiredReports");
		deleteReports(dryRun);
		log.debug("Finished task: checkForExpiredReports");

		log.debug("Running task: checkForExpiredEmployments");
		deleteEmployments(dryRun);
		log.debug("Finished task: checkForExpiredEmployments");

		log.debug("Running task: checkForExpiredPersons");
		deletePersons(dryRun);
		log.debug("Finished task: checkForExpiredPersons");
	}

	public void deleteReports(boolean dryRun) {
		List<Report> reports = reportService.checkForExpiredReports();
		if (!dryRun) {
			reportService.deleteAll(reports);
		} else {
			log.info("Dry run: not deleting any reports. {} reports were found", reports.size());
		}
	}

	public void deleteEmployments(boolean dryRun) {
		ArrayList<Employment> toBeDeleted = new ArrayList<>();
		for (Employment employment : employmentService.findByStopDateBefore(LocalDateTime.now().minusYears(5))) {
			if (!reportService.hasReports(employment)) {
				toBeDeleted.add(employment);
			}
		}

		if (!dryRun) {
			employmentService.deleteAll(toBeDeleted);
			employmentService.deleteAllInAud(toBeDeleted);
		} else {
			log.info("Dry run: not deleting any employments. {} employments were found", toBeDeleted.size());
		}
	}

	public void deletePersons(boolean dryRun) {
		List<Person> persons = checkForExpiredPersons();

		if (!dryRun) {
			personService.deleteAll(persons);
			personService.deleteAllInAud(persons);
		} else {
			log.info("Dry run: not deleting any persons. {} people were found", persons.size());
		}
	}

	public List<Person> checkForExpiredPersons() {
		List<Person> result = new ArrayList<>();

		List<Person> candidatesForDeletion = personService.findByLastEdited(LocalDateTime.now().minusYears(5));
		// Check if they have been edited within the last 5 years
		for (Person person : candidatesForDeletion) {
			if ((person.getEmployments() == null || person.getEmployments().isEmpty()) && !reportService.hasReports(person)) {
				result.add(person);
			}
		}
		return result;
	}
}
