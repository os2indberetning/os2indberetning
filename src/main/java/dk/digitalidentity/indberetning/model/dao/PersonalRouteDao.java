package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalRouteDao extends JpaRepository<PersonalRoute, Long> {
    List<PersonalRoute> findByPersonId(Person person);
}
