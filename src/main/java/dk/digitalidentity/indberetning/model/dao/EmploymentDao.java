package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


    List<Employment> findByStopDateBefore(LocalDateTime stopDate);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM employments_aud WHERE id IN ?1 LIMIT 5000")
    void deleteInAud(@Param("employmentIds") List<Long> employmentIds);

}