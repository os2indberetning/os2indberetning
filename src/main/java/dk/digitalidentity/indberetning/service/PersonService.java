package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.dao.PersonDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {

	private final PersonDao personDao;

	public Person getByCpr(String cpr) {
		return personDao.findByCpr(cpr);
	}

	public Person getById(long id) {
		return personDao.findById(id).orElse(null);
	}

	public List<Person> findByReceiveEmailTrueAndReceivePersonalMailTrue() {
		return personDao.findByReceiveEmailTrueAndReceivePersonalMailTrue();
	}
	public List<Person> getAll() {
		return personDao.findAll();
	}

	public Person save(Person person) {
		return personDao.save(person);
	}
	
	public List<Person> getAllDeadlineNotificationsEnabled() {
		return personDao.findByReceiveEmailAndReceivePersonalMailAndEmailNotNull(true, true);
	}

	public List<Person> save(List<Person> toBeSaved) {
		return personDao.saveAll(toBeSaved);
	}

	public List<Person> findStartingWith(String prefix) {
		// Fetch all potential matches from the database
		Set<Person> persons = personDao.findPersonsByNameOrEmployeeID(prefix);

		// Rank and sort the results based on token-based matching
		Comparator<Person> comparator = Comparator.comparingDouble(person -> calculateTokenMatchScore(person, prefix));
		return persons.stream()
				.sorted(comparator.reversed()) // Sort by match score descending
				.collect(Collectors.toList());
	}

	public double calculateTokenMatchScore(Person person, String query) {
		return SearchUtil.calculateTokenMatchScore(person.getName(), query);
	}

	public List<Person> getAllAdmins() {
		return personDao.findByAdminIsTrue();
	}
}
