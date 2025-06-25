package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteDao extends JpaRepository<Route, Long> {
}
