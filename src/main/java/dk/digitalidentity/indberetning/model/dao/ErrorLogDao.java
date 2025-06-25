package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.ErrorLog;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorLogDao extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByAssignedTo(Person assignedTo);

    List<ErrorLog> findByReport(Report report);

    List<ErrorLog> findBySolutionDateNull();
}
