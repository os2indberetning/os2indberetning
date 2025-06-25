package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Substitute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface SubstituteDao  extends JpaRepository<Substitute, Long> {

	List<Substitute> findByOrgUnit(OrgUnit orgUnit);

	List<Substitute> findByOrgUnitNull();
	List<Substitute> findByOrgUnitNullAndSubstitute(Person substitute);


	List<Substitute> findByOrgUnitNotNull();

	List<Substitute> findBySubstitute(Person substitute);

	boolean existsByOrgUnitAndSubstituteExclusiveModeTrue(OrgUnit orgUnit);
	boolean existsBySubstituteForAndSubstituteAndOrgUnitNull(Person personFor, Person person);
	boolean existsByOrgUnitAndSubstituteExclusiveModeTrueAndSubstituteFor(OrgUnit orgUnit, Person substituteFor);
	boolean existsBySubstituteForAndOrgUnitNull(Person person);
	Set<Substitute> findBySubstituteExclusiveModeTrueAndSubstituteForAndOrgUnitNotNull(Person substituteFor);


	List<Substitute> findBySubstituteForAndOrgUnit(Person person, OrgUnit orgUnit);
	List<Substitute> findBySubstituteForAndOrgUnitNull(Person person);

	List<Substitute> findBySubstituteForAndOrgUnitAndStartDateLessThanEqual(Person substituteFor, OrgUnit orgUnit, LocalDate startDate);
	List<Substitute> findBySubstituteForAndOrgUnitNullAndStartDateLessThanEqual(Person substituteFor, LocalDate startDate);

	List<Substitute> findBySubstituteOrSubstituteFor(Person person, Person person1);

}
