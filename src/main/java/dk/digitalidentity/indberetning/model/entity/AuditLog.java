package dk.digitalidentity.indberetning.model.entity;

import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditlogs")
@Setter
@Getter
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private long id;
	
	@CreationTimestamp
	@Column
	private LocalDateTime tts;
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "auditlogs_details_id", referencedColumnName = "id")
	private AuditLogDetail auditLogDetail;
	
	// referenced entity that performed the action (0 if the performer was system)
	
	@Column
	private Long performerId;
	
	//if performerId = 0, this is SYSTEM
	@Column
	private String performerName;
	
	// action performed
	
	@Column
	@Enumerated(EnumType.STRING)
	private LogAction logAction;
	
	@Column
	private String message;
	
}
