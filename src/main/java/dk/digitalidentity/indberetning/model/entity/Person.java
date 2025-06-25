package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "persons")
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Person {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(nullable = false, length = 10)
	private String cpr;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column
	private String email;

	@Column(nullable = false)
	private boolean active;

	@JsonIgnore
	@OneToMany(mappedBy = "person")
	private List<Employment> employments = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "person")
	@NotAudited
	private List<LicensePlate> licensePlates = new ArrayList<>();

	@Column(nullable = false)
	private boolean admin;

	@Column(nullable = false)
	private boolean receiveEmail;
	@JsonIgnore
	
	@Column(name = "receive_personal_mail")
	private boolean receivePersonalMail;

	@Column(name = "receive_approver_mail")
	private boolean receiveApproverMail;

	@Column(name = "receive_admin_mail")
	private boolean receiveAdminMail;

	@OneToMany(mappedBy = "person")
	@NotAudited
	private Set<Report> reports = new LinkedHashSet<>();
	@JsonIgnore
	@OneToMany(mappedBy = "actualLeader")
	@NotAudited
	private Set<Report> reportsActualLeader = new LinkedHashSet<>();
	@JsonIgnore
	@OneToMany(mappedBy = "approvedBy")
	@NotAudited
	private Set<Report> reportsApproved = new LinkedHashSet<>();
	@JsonIgnore
	@OneToMany(mappedBy = "personId", orphanRemoval = true)
	@NotAudited
	private Set<PersonalRoute> personalRoutes = new LinkedHashSet<>();
	@JsonIgnore
	@OneToMany(mappedBy = "person", orphanRemoval = true)
	@NotAudited
	private Set<Address> addresses = new LinkedHashSet<>();


    @JsonIgnore
	public String getEmployeeNumbers() {
		String result = "";
		if (!employments.isEmpty()) {
			result = employments.stream().map(Employment::getEmployeeNumber).collect(Collectors.joining(", "));
		}
		return result;
	}

	public String getName() {
		return firstName + " " + lastName;
	}
}
