package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {


    @Modifying
    @Query(nativeQuery = true, value = "DELETE dd FROM auditlogs_details dd JOIN (SELECT d.id FROM auditlogs_details d LEFT JOIN auditlogs a ON d.id = a.auditlogs_details_id WHERE a.id IS NULL LIMIT 25000) ss ON ss.id = dd.id")
    void deleteUnreferencedAuditlogDetails();

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM auditlogs WHERE log_action = ?1 AND tts < ?2 LIMIT 25000")
    void deleteByLogActionAndTtsBefore(String logAction, LocalDateTime ttsBefore);

}
