package dk.digitalidentity.indberetning.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "deadline_emails")
public class DeadlineEmails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	String subject;
	
	@Column
	String message;

	@Column
	@Temporal(TemporalType.DATE)
	LocalDate deadline;

	@Column
	@Temporal(TemporalType.DATE)
	LocalDate firstNotification;

	@Column
	@Temporal(TemporalType.DATE)
	LocalDate nextNotification;

	@Column(name = "repeating")
	boolean repeating;

	@Column
	@Temporal(TemporalType.DATE)
	LocalDate lastSent;
}
