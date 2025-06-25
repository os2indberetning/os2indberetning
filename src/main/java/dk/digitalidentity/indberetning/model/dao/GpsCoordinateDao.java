package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface GpsCoordinateDao extends JpaRepository<GpsCoordinate, Long> {
    Optional<GpsCoordinate> findById(long id);

    List<GpsCoordinate> findByReport(Report report);

    List<GpsCoordinate> findByWaypointFalseAndReport(Report report);



    long deleteByReport(Report report);
}
