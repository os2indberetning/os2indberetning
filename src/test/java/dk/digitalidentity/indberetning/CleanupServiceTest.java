package dk.digitalidentity.indberetning;

import dk.digitalidentity.indberetning.model.dao.EmploymentDao;
import dk.digitalidentity.indberetning.model.dao.OrgUnitDao;
import dk.digitalidentity.indberetning.model.dao.PersonDao;
import dk.digitalidentity.indberetning.model.dao.ReportDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.service.CleanupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class CleanupServiceTest {

	@Autowired
	private PersonDao personRepository;

	@Autowired
	private EmploymentDao employmentRepository;

	@Autowired
	private ReportDao reportRepository;

	@Autowired
	private OrgUnitDao orgUnitRepository;

	@Autowired
	private CleanupService cleanupService;

	@PersistenceContext
	private EntityManager entityManager;

	private OrgUnit testOrgUnit;
	private LocalDateTime fiveYearsAgo;
	private LocalDateTime fourYearsAgo;
	private LocalDateTime sixYearsAgo;

	@BeforeEach
	void setUp() {
		// Clean up any existing data
		reportRepository.deleteAll();
		employmentRepository.deleteAll();
		personRepository.deleteAll();
		orgUnitRepository.deleteAll();

		// Set up test dates
		fiveYearsAgo = LocalDateTime.now().minusYears(5).minusDays(1);
		fourYearsAgo = LocalDateTime.now().minusYears(4);
		sixYearsAgo = LocalDateTime.now().minusYears(6);

		// Create a test OrgUnit (assuming you have this entity)
		testOrgUnit = createTestOrgUnit();
		orgUnitRepository.save(testOrgUnit);
	}

	@Test
	void shouldDeleteReportsOlderThanFiveYears_AllDatesOld() {
		// Given: A person with employment and a report where ALL dates are older than 5 years
		Person person = createTestPerson("1234567890", LocalDateTime.now());

		Employment employment = createTestEmployment(person, null);
		employmentRepository.save(employment);

		Report oldReport = createAndSaveTestReport(person, employment, sixYearsAgo, sixYearsAgo, sixYearsAgo, sixYearsAgo.toLocalDate());

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Report should be deleted
		assertThat(reportRepository.findById(oldReport.getId())).isEmpty();
	}

	@Test
	void shouldNotDeleteReports_WhenAnyDateIsRecent() {
		// Given: A person with employment and reports where at least one date is recent
		Person person = createTestPerson("1234567891", LocalDateTime.now());

		Employment employment = createTestEmployment(person, null);
		employmentRepository.save(employment);

		// Report with recent created date
		Report reportWithRecentCreated = createAndSaveTestReport(person, employment, fourYearsAgo, sixYearsAgo, sixYearsAgo, sixYearsAgo.toLocalDate());

		// Report with recent closed date
		Report reportWithRecentClosed = createAndSaveTestReport(person, employment, sixYearsAgo, fourYearsAgo, sixYearsAgo, sixYearsAgo.toLocalDate());

		// Report with recent processed date
		Report reportWithRecentProcessed = createAndSaveTestReport(person, employment, sixYearsAgo, sixYearsAgo, fourYearsAgo, sixYearsAgo.toLocalDate());

		// Report with recent drive date
		Report reportWithRecentDrive = createAndSaveTestReport(person, employment, sixYearsAgo, sixYearsAgo, sixYearsAgo, fourYearsAgo.toLocalDate());

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: All reports should still exist
		assertThat(reportRepository.findById(reportWithRecentCreated.getId())).isPresent();
		assertThat(reportRepository.findById(reportWithRecentClosed.getId())).isPresent();
		assertThat(reportRepository.findById(reportWithRecentProcessed.getId())).isPresent();
		assertThat(reportRepository.findById(reportWithRecentDrive.getId())).isPresent();
	}

	@Test
	void shouldDeleteEmploymentsWithoutReports_WhenStopDateIsOld() {
		// Given: A person with employment that has no reports and old stop date
		Person person = createTestPerson("1234567892", LocalDateTime.now());

		Employment oldEmployment = createTestEmployment(person, fiveYearsAgo);
		employmentRepository.save(oldEmployment);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Employment should be deleted
		assertThat(employmentRepository.findById(oldEmployment.getId())).isEmpty();
	}

	@Test
	void shouldNotDeleteEmployments_WhenTheyHaveReports() {
		// Given: A person with employment that has reports (even if stop date is old)
		Person person = createTestPerson("1234567893", LocalDateTime.now());

		Employment employment = createTestEmployment(person, fiveYearsAgo);
		employmentRepository.save(employment);

		// TODO: something wrong here, this needs to be saved?
		Report report = createAndSaveTestReport(person, employment, fourYearsAgo, fourYearsAgo, fourYearsAgo, fourYearsAgo.toLocalDate());

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Employment should still exist (it has reports)
		assertThat(employmentRepository.findById(employment.getId())).isPresent();
	}

	@Test
	void shouldNotDeleteEmployments_WhenStopDateIsRecent() {
		// Given: A person with employment that has a recent stop date (no reports)
		Person person = createTestPerson("1234567894", LocalDateTime.now());

		Employment recentEmployment = createTestEmployment(person, fourYearsAgo);
		employmentRepository.save(recentEmployment);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Employment should still exist (stop date is recent)
		assertThat(employmentRepository.findById(recentEmployment.getId())).isPresent();
	}

	@Test
	void shouldNotDeleteEmployments_WhenStopDateIsNull() {
		// Given: A person with active employment (no stop date, no reports)
		Person person = createTestPerson("1234567895", LocalDateTime.now());

		Employment activeEmployment = createTestEmployment(person, null);
		employmentRepository.save(activeEmployment);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Employment should still exist (no stop date = active)
		assertThat(employmentRepository.findById(activeEmployment.getId())).isPresent();
	}

	@Test
	void shouldDeleteOldPersonsWithoutReportsAndEmployments() {
		// Given: A person with no reports and no employments
		Person person = createTestPerson("1234567896", fiveYearsAgo);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Person should be deleted
		assertThat(personRepository.findById(person.getId())).isEmpty();
	}

	@Test
	void shouldNotDeleteNewPersonsWithoutReportsAndEmployments() {
		// Given: A person with no reports and no employments
		Person person = createTestPerson("1234567896", fourYearsAgo);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Person should be deleted
		assertThat(personRepository.findById(person.getId())).isPresent();
	}

	@Test
	void shouldNotDeletePersons_WhenTheyHaveEmployments() {
		// Given: A person with employments but no reports
		Person person = createTestPerson("1234567898", fiveYearsAgo);
		personRepository.save(person);

		Employment employment = createTestEmployment(person, null);
		employmentRepository.save(employment);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Person should still exist (they have employments)
		assertThat(personRepository.findById(person.getId())).isPresent();
	}

	@Test
	void shouldHandleComplexCleanupScenario() {
		// Given: Complex scenario with multiple persons, employments, and reports

		// Person 1: Should be completely deleted (no reports, old employment)
		Person person1 = createTestPerson("1111111111", fiveYearsAgo);
		personRepository.save(person1);
		Employment employment1 = createTestEmployment(person1, fiveYearsAgo);
		employmentRepository.save(employment1);

		// Person 2: Should keep person and employment (recent reports)
		Person person2 = createTestPerson("2222222222", fiveYearsAgo);
		personRepository.save(person2);
		Employment employment2 = createTestEmployment(person2, fiveYearsAgo);
		employmentRepository.save(employment2);
		Report recentReport = createAndSaveTestReport(person2, employment2, fourYearsAgo, fourYearsAgo, fourYearsAgo, fourYearsAgo.toLocalDate());

		// Person 3: Should delete old reports but keep person and employment
		Person person3 = createTestPerson("3333333333", fiveYearsAgo);
		personRepository.save(person3);
		Employment employment3 = createTestEmployment(person3, null);
		employmentRepository.save(employment3);
		Report oldReport = createAndSaveTestReport(person3, employment3, sixYearsAgo, sixYearsAgo, sixYearsAgo, sixYearsAgo.toLocalDate());
		Report newReport = createAndSaveTestReport(person3, employment3, fourYearsAgo, fourYearsAgo, fourYearsAgo, fourYearsAgo.toLocalDate());

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Verify expected cleanup results
		// Person 1 and employment 1 should be deleted
		assertThat(personRepository.findById(person1.getId())).isEmpty();
		assertThat(employmentRepository.findById(employment1.getId())).isEmpty();

		// Person 2 and employment 2 should still exist, with their recent report
		assertThat(personRepository.findById(person2.getId())).isPresent();
		assertThat(employmentRepository.findById(employment2.getId())).isPresent();
		assertThat(reportRepository.findById(recentReport.getId())).isPresent();

		// Person 3 and employment 3 should still exist, old report deleted, new report kept
		assertThat(personRepository.findById(person3.getId())).isPresent();
		assertThat(employmentRepository.findById(employment3.getId())).isPresent();
		assertThat(reportRepository.findById(oldReport.getId())).isEmpty();
		assertThat(reportRepository.findById(newReport.getId())).isPresent();
	}

	@Test
	void shouldHandleEmptyDatabase() {
		// Given: Empty database (setUp already cleans it)
		assertThat(personRepository.count()).isEqualTo(0);
		assertThat(employmentRepository.count()).isEqualTo(0);
		assertThat(reportRepository.count()).isEqualTo(0);

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Should not throw exceptions and database should remain empty
		assertThat(personRepository.count()).isEqualTo(0);
		assertThat(employmentRepository.count()).isEqualTo(0);
		assertThat(reportRepository.count()).isEqualTo(0);
	}

	@Test
	void shouldHandleBoundaryDates() {
		// Given: Reports with dates exactly at the 5-year boundary
		Person person = createTestPerson("5555555555", fiveYearsAgo);
		personRepository.save(person);

		Employment employment = createTestEmployment(person, null);
		employmentRepository.save(employment);

		LocalDateTime exactlyFiveYears = LocalDateTime.now().minusYears(5);
		LocalDateTime justOverFiveYears = LocalDateTime.now().minusYears(5).minusDays(1);

		Report exactBoundaryReport = createAndSaveTestReport(person, employment, exactlyFiveYears, exactlyFiveYears, exactlyFiveYears, exactlyFiveYears.toLocalDate());

		System.out.println("justOverFiveYears = " + justOverFiveYears);
		System.out.println("justOverFiveYears.toLocalDate() = " + justOverFiveYears.toLocalDate());

		Report justOverBoundaryReport = createAndSaveTestReport(person, employment, justOverFiveYears, justOverFiveYears, justOverFiveYears, justOverFiveYears.toLocalDate());

		// When: Running cleanup
		cleanupService.cleanup(false);

		// Then: Only the report just over 5 years should be deleted
		assertThat(reportRepository.findById(exactBoundaryReport.getId())).isPresent();
		assertThat(reportRepository.findById(justOverBoundaryReport.getId())).isEmpty();
	}

	// Helper methods
	private Person createTestPerson(String cpr, LocalDateTime lastEdited) {
		Person person = new Person();
		person.setCpr(cpr);
		person.setFirstName("Test");
		person.setLastName("Person");
		person.setEmail("test@example.com");
		person.setActive(true);
		person.setAdmin(false);
		person.setReceiveEmail(false);
		person.setReceivePersonalMail(false);
		person.setReceiveApproverMail(false);
		person.setReceiveAdminMail(false);

		// Save first to let @UpdateTimestamp set the lastEdited
		Person savedPerson = personRepository.save(person);

		// Force flush to ensure the save is committed
		entityManager.flush();

		// Update the created_date directly using native SQL to bypass Hibernate's timestamp management
		entityManager.createNativeQuery("UPDATE persons SET last_edited = ?1 WHERE id = ?2")
				.setParameter(1, lastEdited)
				.setParameter(2, savedPerson.getId())
				.executeUpdate();

		// Clear the persistence context to ensure we get fresh data
		entityManager.clear();

		// Return the updated report
		return personRepository.findById(savedPerson.getId()).orElseThrow();
	}

	private Employment createTestEmployment(Person person, LocalDateTime stopDate) {
		return Employment.builder().person(person).employeeNumber("EMP123").position("Test Position").leader(false).startDate(LocalDateTime.now().minusYears(10)).stopDate(stopDate).employmentType("FULL_TIME").orgUnit(testOrgUnit).extraNumber(0).costCenter(12345L).institutionCode("INST123").build();
	}

	/**
	 * Creates and saves a test report with the specified dates. This method handles the @CreationTimestamp annotation by: 1. Creating and saving the report (letting @CreationTimestamp set createdDate) 2. Using a native SQL query to update the created_date to our desired test value
	 */
	private Report createAndSaveTestReport(Person person, Employment employment, LocalDateTime createdDate, LocalDateTime closedDate, LocalDateTime processedDate, LocalDate driveDate) {
		Report report = new Report();
		report.setPerson(person);
		report.setEmployment(employment);
		report.setStatus(ReportStatus.INVOICED);

		// Note: createdDate will be set by @CreationTimestamp when we save
		report.setClosedDate(closedDate);
		report.setDriveDate(driveDate);
		report.setProcessedDate(processedDate);

		report.setFullName(person.getFirstName() + " " + person.getLastName());
		report.setEmployeeNumber(employment.getEmployeeNumber());
		report.setPurpose("Test Purpose");
		report.setRawDistance(100.0);
		report.setRoundTrip(false);
		report.setKmRateType("STANDARD");
		report.setKmRate(3.5);
		report.setFourKmRule(false);
		report.setFromApp(false);
		report.setDistance(100.0);
		report.setAmountToReimburse(350.0);
		report.setActiveYear(2020);

		// Save first to let @CreationTimestamp set the createdDate
		Report savedReport = reportRepository.save(report);

		// Force flush to ensure the save is committed
		entityManager.flush();

		// Update the created_date directly using native SQL to bypass Hibernate's timestamp management
		entityManager.createNativeQuery("UPDATE reports SET created_date = ?1 WHERE id = ?2")
				.setParameter(1, createdDate)
				.setParameter(2, savedReport.getId())
				.executeUpdate();

		// Clear the persistence context to ensure we get fresh data
		entityManager.clear();

		// Return the updated report
		return reportRepository.findById(savedReport.getId()).orElseThrow();
	}

	private OrgUnit createTestOrgUnit() {
		OrgUnit orgUnit = new OrgUnit();
		orgUnit.setOrgId("OU_TEST_01");
		orgUnit.setLongDescription("OU");
		orgUnit.setShortDescription("OU");
		orgUnit.setDefaultCalculationType(CalculationType.CALCULATED);
		orgUnit.setFourKmRuleAllowed(false);

		return orgUnit;
	}
}
