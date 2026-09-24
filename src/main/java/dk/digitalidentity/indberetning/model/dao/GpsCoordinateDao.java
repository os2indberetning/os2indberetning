package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface GpsCoordinateDao extends JpaRepository<GpsCoordinate, Long> {
    Optional<GpsCoordinate> findById(long id);

    List<GpsCoordinate> findByReport(Report report);

    List<GpsCoordinate> findByWaypointFalseAndReport(Report report);


    @Query(nativeQuery = true, value = """
    SELECT gps.address
    FROM gps_coordinates gps
    WHERE gps.report_id = ?1
    ORDER BY gps.point_number ASC
    """)
    List<String> findAddressesByReportId(long id);

    long deleteByReport(Report report);
}
