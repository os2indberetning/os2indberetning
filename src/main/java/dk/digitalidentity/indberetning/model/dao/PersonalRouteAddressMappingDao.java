package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.PersonalRouteAddressMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PersonalRouteAddressMappingDao extends JpaRepository<PersonalRouteAddressMapping, Long> {
    List<PersonalRouteAddressMapping> findByPersonalRoute(PersonalRoute personalRoute);
}
