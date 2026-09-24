package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Audited
@EntityListeners(AuditingEntityListener.class)
@Table(name = "employments")
public class Employment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "employee_number")
	private String employeeNumber;

	@Column(name = "position")
	private String position;

	@ManyToOne
	@JoinColumn(name = "person_id")
	private Person person;

	@Column(name = "leader", nullable = false)
	private boolean leader;

	@Column(name = "start_date")
	private LocalDateTime startDate;

	@Column(name = "stop_date")
	private LocalDateTime stopDate;

	@Column(name = "employment_type")
	private String employmentType;

	@ManyToOne(optional = false)
	@JoinColumn(name = "orgunit_id", nullable = false)
	private OrgUnit orgUnit;

	@Column(name = "extra_number", nullable = false)
	private int extraNumber;

	@Column(name = "cost_center", nullable = false)
	private long costCenter;

	@Column(name = "institution_code")
	private String institutionCode;

	@JsonIgnore
	@NotAudited
	@OneToMany(mappedBy = "employment")
	private Set<Report> reports = new LinkedHashSet<>();

	@Column(name = "home_to_work_distance_override", nullable = true)
	private Double homeToWorkDistanceOverride;
}