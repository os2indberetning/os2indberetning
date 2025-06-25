package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PersonDao extends JpaRepository<Person, Long> {
	Person findByCpr(String cpr);
    List<Person> findByFirstNameStartsWithOrLastNameStartsWith(String firstNamePrefix, String lastNamePrefix);

	List<Person> findByReceiveEmailAndReceivePersonalMailAndEmailNotNull(boolean receiveEmail, boolean receivePersonalMail);
	List<Person> findByAdminIsTrue();

	@Query(nativeQuery = true, value =
            """
            SELECT p.*
            FROM persons p
            JOIN employments e ON
               p.id = e.person_id
            WHERE e.employee_number LIKE CONCAT('%', ?1, '%')
               OR p.first_name LIKE CONCAT('%', ?1, '%')
               OR p.last_name LIKE CONCAT('%', ?1, '%')
               OR CONCAT(p.first_name, ' ', p.last_name) LIKE CONCAT('%', ?1, '%')
            GROUP BY p.id
            """)
    Set<Person> findPersonsByNameOrEmployeeID(@Param("id") String input);

	List<Person> findByReceiveEmailTrueAndReportsNotEmpty();

	List<Person> findByReceiveEmailTrueAndReceivePersonalMailTrue();
}
