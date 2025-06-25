package dk.digitalidentity.indberetning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.digitalidentity.indberetning.model.dao.AuditLogDao;
import dk.digitalidentity.indberetning.model.entity.AuditLog;
import dk.digitalidentity.indberetning.model.entity.AuditLogDetail;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
	private final AuditLogDao auditLogDao;

	public void save(AuditLog log) { auditLogDao.save(log);}

	/**
	 *
	 * @param performerId set to 0 for SYSTEM
	 * @param performerName if performer id is 0 this will always be SYSTEM
	 * @param logAction
	 * @param msg used for special stuff to be aware of.
	 * @param obj
	 */
	public void saveSystem(LogAction logAction, String msg, Object obj) {
		save(0L, "SYSTEM", logAction, msg, obj);
	}
	public void save(Person person, LogAction logAction, String msg, Object obj) {
		save(person.getId(), person.getName(), logAction, msg, obj);
	}

	public void save(long performerId, String performerName, LogAction logAction, String msg, Object obj) {
		AuditLog auditLog = new AuditLog();

		if (obj != null) {
			try {
				AuditLogDetail ad =  new AuditLogDetail();
				ad.setContent(objectToJson(obj));
				auditLog.setAuditLogDetail(ad);
			} catch (JsonProcessingException e) {
				var errorDetail = new AuditLogDetail();
				errorDetail.setContent("{\"error\":\"Failed to convert data to json due to: " + e.getMessage() + "\"}");
				auditLog.setAuditLogDetail(errorDetail);

			}
		}

		auditLog.setPerformerId(performerId);
		if(performerId == 0) {
			auditLog.setPerformerName("SYSTEM");
		} else {
			auditLog.setPerformerName(performerName);
		}
		auditLog.setLogAction(logAction);
		auditLog.setMessage(msg);

		auditLogDao.save(auditLog);
	}
	public void saveAll(List<AuditLog> logs) {
		auditLogDao.saveAll(logs);
	}

	private String objectToJson(Object object) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteUnreferencedAuditlogDetails() {
		auditLogDao.deleteUnreferencedAuditlogDetails();
	}

	@Transactional(rollbackFor = Exception.class)
	public void cleanUpOldAuditLogs() {
		for (LogAction value : LogAction.values()) {
            switch (value.getRetentionLevel()) {
                case FOREVER -> {
					continue;
                }
                case FIVE_YEARS -> {
					LocalDateTime fiveYears = LocalDateTime.now().minusYears(5);
					auditLogDao.deleteByLogActionAndTtsBefore(value.name(), fiveYears);
				}
                case ONE_YEAR -> {
					LocalDateTime oneYear = LocalDateTime.now().minusYears(1);
					auditLogDao.deleteByLogActionAndTtsBefore(value.name(), oneYear);
                }
                case ONE_MONTH -> {
					LocalDateTime oneMonth = LocalDateTime.now().minusMonths(1);
					auditLogDao.deleteByLogActionAndTtsBefore(value.name(), oneMonth);
                }
            }
		}
	}

}
