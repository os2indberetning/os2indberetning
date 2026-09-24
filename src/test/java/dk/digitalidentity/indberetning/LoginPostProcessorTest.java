package dk.digitalidentity.indberetning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.IdentityProviderConfiguration;
import dk.digitalidentity.indberetning.model.dao.EmploymentDao;
import dk.digitalidentity.indberetning.model.dao.OrgUnitDao;
import dk.digitalidentity.indberetning.model.dao.PersonDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.security.Roles;
import dk.digitalidentity.indberetning.security.enums.ClaimConstants;
import dk.digitalidentity.indberetning.security.enums.IdentityProviderType;
import dk.digitalidentity.indberetning.security.enums.MitidConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Testcontainers
@Transactional
public class LoginPostProcessorTest {

	@Autowired
	private OS2indberetningConfiguration configuration;

	@Autowired
	private SamlLoginPostProcessor loginPostProcessor;

	@Autowired
	private PersonDao personDao; 

	@Autowired
	private EmploymentDao employmentDao;

	@Autowired
	private OrgUnitDao orgUnitDao;

	@PersistenceContext
	private EntityManager entityManager;

	private OrgUnit orgUnit;

	private static final String TEST_ADMIN_CPR = "0000000000";
	private static final String TEST_APPROVER_CPR = "0000000001";
	private static final String TEST_USER_CPR = "0000000002";

	@BeforeEach
	void setup() {
		configuration.getIdps().put(IdentityProviderType.MITID, new IdentityProviderConfiguration("mitid_test", ""));
		configuration.getIdps().put(IdentityProviderType.NORMAL, new IdentityProviderConfiguration("normal_test", ""));

		personDao.deleteAll();
		orgUnitDao.deleteAll();
		employmentDao.deleteAll();

		orgUnit = new OrgUnit();
		orgUnit.setOrgId("OU_TEST_01");
		orgUnit.setLongDescription("OU");
		orgUnit.setShortDescription("OU");
		orgUnit.setDefaultCalculationType(CalculationType.CALCULATED);
		orgUnit.setFourKmRuleAllowed(false);

		orgUnitDao.save(orgUnit);

		var admin = createTestPerson(TEST_ADMIN_CPR, true);
		createTestApproverEmployment(admin, null, false);

		var approver = createTestPerson(TEST_APPROVER_CPR, false);
		createTestApproverEmployment(approver, null, true);

		var user = createTestPerson(TEST_USER_CPR, false);
		createTestApproverEmployment(user, null, false);
	}

	@Test
	void testNonExistingUser() {
		var tokenUser = TokenUser.builder()
				.attributes(new HashMap<String, Object>())
				.authorities(new ArrayList<SamlGrantedAuthority>())
				.issuer(configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())
				.username("9999999999")
				.build();

		loginPostProcessor.process(tokenUser);

		assertThat(tokenUser.getAuthorities().isEmpty()).isTrue();
	}

	@Test
	void testAdminLogin() {
		var attributes = new HashMap<String, Object>();
		var authorities = new ArrayList<SamlGrantedAuthority>();

		authorities.add(new SamlGrantedAuthority(Roles.ROLE_ADMINISTRATOR));

		var tokenUser = TokenUser.builder()
				.attributes(attributes)
				.authorities(authorities)
				.issuer(configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())
				.username(TEST_ADMIN_CPR)
				.build();

		loginPostProcessor.process(tokenUser);

		assertThat(tokenUser.getAuthorities().size() == 2).isTrue();
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_USER))).isTrue();
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_ADMINISTRATOR))).isTrue();
	}

	@Test
	void testApproverLogin() {
		var attributes = new HashMap<String, Object>();
		var authorities = new ArrayList<SamlGrantedAuthority>();

		var tokenUser = TokenUser.builder()
				.attributes(attributes)
				.authorities(authorities)
				.issuer(configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())
				.username(TEST_APPROVER_CPR)
				.build();

		loginPostProcessor.process(tokenUser);

		assertThat(tokenUser.getAuthorities().size()).isEqualTo(2L);
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_USER))).isTrue();
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_APPROVER))).isTrue();
	}

	@Test
	void testMitidPersonalLogin() {
		var attributes = new HashMap<String, Object>();
		attributes.put(MitidConstants.PERSONAL_CPR, TEST_USER_CPR);

		var tokenUser = TokenUser.builder()
				.attributes(attributes)
				.authorities(new ArrayList<SamlGrantedAuthority>())
				.issuer(configuration.getIdps().get(IdentityProviderType.MITID).getEntityId())
				.build();

		loginPostProcessor.process(tokenUser);

		assertThat(tokenUser.getAuthorities().size()).isEqualTo(1L);
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_USER))).isTrue();
	}

	@Test
	void testEmailChangeOnLogin() {
		var attributes = new HashMap<String, Object>();
		var authorities = new ArrayList<SamlGrantedAuthority>();

		final String NEW_EMAIL = "enellerandentestemail@email.test";

		attributes.put(ClaimConstants.EMAIL_ADDRESS, NEW_EMAIL);

		var tokenUser = TokenUser.builder()
				.attributes(attributes)
				.authorities(authorities)
				.issuer(configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())
				.username(TEST_USER_CPR)
				.build();

		loginPostProcessor.process(tokenUser);


		assertThat(personDao.findByCpr(TEST_USER_CPR).getEmail()).isEqualTo(NEW_EMAIL);
	}

	@Test
	void testAdminStatusRemoved() {
		var attributes = new HashMap<String, Object>();
		var authorities = new ArrayList<SamlGrantedAuthority>();
		var person = personDao.findByCpr(TEST_ADMIN_CPR);

		person.setAdmin(true);

		personDao.save(person);

		var tokenUser = TokenUser.builder()
				.attributes(attributes)
				.authorities(authorities)
				.issuer(configuration.getIdps().get(IdentityProviderType.NORMAL).getEntityId())
				.username(TEST_ADMIN_CPR)
				.build();

		loginPostProcessor.process(tokenUser);

		assertThat(tokenUser.getAuthorities().size() == 1).isTrue();
		assertThat(tokenUser.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_USER))).isTrue();
	}

	private Employment createTestApproverEmployment(Person person, LocalDateTime stopDate, boolean leader) {
		var employment = Employment.builder().person(person).employeeNumber("EMP123").position("Test Position").leader(leader).startDate(LocalDateTime.now().minusYears(10)).stopDate(stopDate).employmentType("FULL_TIME").orgUnit(orgUnit).extraNumber(0).costCenter(12345L).institutionCode("INST123").build();
		employmentDao.save(employment);
		return employment;
	}

	private Person createTestPerson(String cpr, boolean admin) {
		Person person = new Person();
		person.setCpr(cpr);
		person.setFirstName("Test");
		person.setLastName("Person");
		person.setEmail("test@example.com");
		person.setActive(true);
		person.setAdmin(admin);
		person.setReceiveEmail(false);
		person.setReceivePersonalMail(false);
		person.setReceiveApproverMail(false);
		person.setReceiveAdminMail(false);

		// Save first to let @UpdateTimestamp set the lastEdited
		Person savedPerson = personDao.save(person);

		// Force flush to ensure the save is committed
		entityManager.flush();

		// Update the created_date directly using native SQL to bypass Hibernate's timestamp management
		entityManager.createNativeQuery("UPDATE persons SET last_edited = ?1 WHERE id = ?2")
				.setParameter(1, LocalDateTime.now())
				.setParameter(2, savedPerson.getId())
				.executeUpdate();

		// Clear the persistence context to ensure we get fresh data
		entityManager.clear();

		// Return the updated report
		return personDao.findById(savedPerson.getId()).orElseThrow();
	
	}
}
