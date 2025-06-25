package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "substitutes")
public class Substitute {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="start_date")
	private LocalDate startDate;

	@Column(name="end_date", nullable = true)
	private LocalDate endDate;

	@ManyToOne(optional = false)
	@JsonIgnore
	@JoinColumn(name = "substitute", nullable = false)
	private Person substitute;

	@ManyToOne(optional = false)
	@JsonIgnore
	@JoinColumn(name = "substitute_for", nullable = false)
	private Person substituteFor;

	@ManyToOne(optional = true)
	@JsonIgnore
	@JoinColumn(name = "org_unit", nullable = true)
	private OrgUnit orgUnit;

	@Column(name="substitute_exclusive_mode")
	private boolean substituteExclusiveMode;

	@OneToOne(optional = false)
	@JsonIgnore
	@JoinColumn(name = "created_by", nullable = false)
	private Person createdBy;

	@Column(name = "finished")
	private boolean finished;
}