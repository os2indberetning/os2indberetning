package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmploymentDao extends JpaRepository<Employment, Long> {
    Optional<Employment> findById(long id);
    List<Employment> findByPerson(Person person);

    List<Employment> findByPersonAndOrgUnit(Person person, OrgUnit orgUnit);

    Set<Employment> findByIdIn(Set<Long> ids);

    List<Employment> findByStopDateBeforeOrStopDate(LocalDateTime date, LocalDateTime date2);

    List<Employment> findByStopDateAfterOrStopDateNull(LocalDateTime date);

}