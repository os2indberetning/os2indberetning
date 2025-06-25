package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Audited
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orgunits")
public class OrgUnit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "org_id")
	private String orgId;

	@Column(name = "short_description")
	private String shortDescription;

	@Column(name = "long_description")
	private String longDescription;

	@Column(name = "four_km_rule_allowed")
	private Boolean fourKmRuleAllowed;

	@Column(name = "calculation_type")
	@Enumerated(EnumType.STRING)
	private CalculationType defaultCalculationType;

	@ManyToOne
	@JoinColumn(name = "parent_id")
	private OrgUnit parent;
	@JsonIgnore
	@OneToMany(mappedBy = "parent")
	private Set<OrgUnit> children = new LinkedHashSet<>();

	@JsonIgnore
	@OneToMany(mappedBy = "orgUnit")
	private Set<Employment> employments = new LinkedHashSet<>();
	@JsonIgnore
	@OneToMany(mappedBy = "orgUnit", orphanRemoval = true)
	@NotAudited
	private Set<Address> addresses = new LinkedHashSet<>();

}