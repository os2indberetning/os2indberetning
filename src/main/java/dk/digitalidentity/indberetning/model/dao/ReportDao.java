package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
}
