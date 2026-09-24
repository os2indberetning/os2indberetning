package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReportDao extends JpaRepository<Report, Long> {
    List<Report> findByRecalculateTrue();
    List<Report> findByPersonAndDriveDateBetweenAndStatus(Person person, LocalDate driveDateStart, LocalDate driveDateEnd, ReportStatus status);
    List<Report> findByClosedDate(LocalDateTime closedDate);
    List<Report> findByCreatedDate(LocalDateTime createdDate);
    List<Report> findByDriveDate(LocalDateTime driveDate);
    List<Report> findByEditedDate(LocalDateTime editedDate);
    List<Report> findByProcessedDate(LocalDateTime processedDate);
    List<Report> findByActualLeaderId(Person person);
    List<Report> findByApprovedById(Person person);
    List<Report> findByPersonId(Person person);
    List<Report> findByStatus(ReportStatus status);
    List<Report> findByStatus(ReportStatus status, Pageable pageable);
    List<Report> findByPersonAndNotificationSentFalseAndStatus(Person person, ReportStatus reportStatus);

    List<Report> findByPersonAndDriveDate(Person person, LocalDate driveDate);
    List<Report> findByEmployment_OrgUnitIn(Collection<OrgUnit> orgUnits);
    List<Report> findByPersonAndStatus(Person person, ReportStatus status);
    List<Report> findByPersonAndStatus(Person person, ReportStatus status, Pageable pageable);
    List<Report> findByStatusAndProcessedDateNull(ReportStatus status);
    @Modifying
    @Query(nativeQuery = true, value = "SELECT DISTINCT person_id FROM reports WHERE person_id = ?1 AND status = ?2 GROUP BY employment_id")
    List<Report> findByDistinctPerson(Person person, ReportStatus status);

    List<Report> findByStatusAndProcessedDateNullAndDistanceGreaterThan(ReportStatus status, double distance);

    List<Report> findByStatusAndClosedDateAfterAndNotificationSentFalse(ReportStatus status, LocalDateTime localDateTime);
    List<Report> findByPerson_IdAndEmployment_EmployeeNumberAndDriveDateBetween(long personId, String employeeNumber, LocalDate driveDateStart, LocalDate driveDateEnd);
    List<Report> findByPerson_IdAndEmployeeNumberAndDriveDateBetween(long id, String employeeNumber, LocalDate driveDateStart, LocalDate driveDateEnd);
    List<Report> findByPersonAndDriveDateBetweenAndCoords_EndPointTrueAndCoords_Address(Person person, LocalDate driveDateStart, LocalDate driveDateEnd, String address);

    List<Report> findByIdIn(Collection<Long> ids);

    List<Report> findByIdInAndStatus(Collection<Long> ids, ReportStatus status);

    List<Report> findByPerson_IdIn(Collection<Long> ids);

    List<Report> findByPerson_IdInAndStatus(Collection<Long> ids, ReportStatus status);

    List<Report> findByEmployment_IdIn(Collection<Long> ids);
    List<Report> findByEmployment_IdInAndStatus(Collection<Long> ids, ReportStatus status);

    List<Report> findByClosedDateAfter(LocalDateTime closedDate);

	Optional<Report> findFirstByPersonOrderByCreatedDateDesc(Person person);

    List<Report> findByProcessedDateBefore(LocalDateTime date);


    List<Report> findByPersonAndDriveDateAndStatusNot(Person person, LocalDate driveDate, ReportStatus status);

    boolean existsByPerson(Person person);

    boolean existsByEmployment(Employment employment);

	List<Report> findByCreatedDateBeforeAndProcessedDateBeforeAndClosedDateBeforeAndDriveDateBefore(LocalDateTime createdDate, LocalDateTime processedDate, LocalDateTime closedDate, LocalDate driveDate);
    List<Report> findByPersonAndDriveDateAndStatusNotAndStatusNot(Person person, LocalDate driveDate, ReportStatus status, ReportStatus status1);

    @Query("""
            SELECT r.id FROM Report r
            JOIN r.employment e
            JOIN e.orgUnit
            WHERE r.status IN :statuses
              AND r.driveDate BETWEEN :from AND :to
            """)
    Page<Long> findIdsByStatusInAndDriveDateBetween(
            @org.springframework.data.repository.query.Param("statuses") Collection<ReportStatus> statuses,
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT r.id FROM Report r
            JOIN r.employment e
            JOIN e.orgUnit ou
            WHERE r.status IN :statuses
              AND r.driveDate BETWEEN :from AND :to
              AND ou.id = :orgUnitId
            """)
    Page<Long> findIdsByStatusInAndDriveDateBetweenAndOrgUnit(
            @org.springframework.data.repository.query.Param("statuses") Collection<ReportStatus> statuses,
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to,
            @org.springframework.data.repository.query.Param("orgUnitId") long orgUnitId,
            Pageable pageable);

    @Query("""
            SELECT r FROM Report r
            JOIN FETCH r.coords
            JOIN FETCH r.employment e
            JOIN FETCH e.orgUnit
            WHERE r.id IN :ids
            """)
    List<Report> findWithCoordsByIdIn(
            @org.springframework.data.repository.query.Param("ids") Collection<Long> ids);

    @Query("SELECT r.id FROM Report r WHERE r.route IS NOT NULL AND r.id IN :reportIds")
	Set<Long> findRouteReportIds(@org.springframework.data.repository.query.Param("reportIds") Collection<Long> reportIds);
}
