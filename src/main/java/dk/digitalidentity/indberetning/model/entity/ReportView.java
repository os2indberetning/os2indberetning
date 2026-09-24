package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.jpa.domain.Specification;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@ToString
@Entity(name = "view_reports")
public class ReportView {
	@Id
	@Column
	@JsonProperty("id")
	private long id;
	
	@Column
	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	private ReportStatus status;
	
	@Column(name = "person_id")
	private long personId;
	
	@Column
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonProperty("createdDate")
	private LocalDateTime createdDate;
	
	@Column
	@JsonFormat(pattern = "yyyy-MM-dd")
	@JsonProperty("driveDate")
	private LocalDate driveDate;
	
	@Column(name = "full_name")
	@JsonProperty("fullName")
	private String fullName;
	
	@Column
	@JsonProperty("employeeNumber")
	private long employeeNumber;
	
	@Column
	@JsonProperty("approvedByName")
	private String approvedByName;

	@JsonProperty("potentialApprovers")
	@Column(name = "potential_approvers")
	private String potentialApprovers;
	
	@Column
	@JsonProperty("purpose")
	private String purpose;
	
	@Column
	@JsonProperty("kmRateType")
	private String kmRateType;
	
	@Column
	@JsonProperty("comment")
	private String comment;
	
	@Column
	@JsonProperty("distance")
	private double distance;
	
	@Column
	@JsonProperty("amountToReimburse")
	private double amountToReimburse;
	
	@Column(name = "is_extra_distance")
	@JsonProperty("extraDistance")
	private boolean extraDistance;

    @Column(name = "extra_distance_amount")
    @JsonProperty("extraDistanceAmount")
    private double extraDistanceAmount;

	@Column
	@JsonProperty("fourKmRule")
	private boolean fourKmRule;
	
	@Column(name = "orgunit_initials")
	private String orgunitIntials;

	@JsonProperty("orgunitName")
	@Column(name = "orgunit_name")
	private String orgunitName;

	@JsonProperty("routeGeometry")
	@Column(name = "route_geometry")
	private String routeGeometry;
	
	@JsonProperty("fromApp")
	@Column(name = "from_app")
	private boolean fromApp;
	
	@JsonProperty("divergentAddress")
	@Column(name = "divergent_address")
	private boolean divergentAddress;

	@JsonProperty("roundTrip")
	@Column(name = "round_trip")
	private boolean roundTrip;

	@JsonProperty("userComment")
	@Column(name = "user_comment")
	private String userComment;

	@JsonProperty("startsAtHome")
	@Column(name = "starts_at_home")
	private boolean startsAtHome;

	@JsonProperty("endsAtHome")
	@Column(name = "ends_at_home")
	private boolean endsAtHome;

	@Column
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonProperty("processedDate")
	private LocalDateTime processedDate;

	@Column
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonProperty("closedDate")
	private LocalDateTime closedDate;

	@Column
	@JsonProperty("error_code")
	private String errorCode;

	public static Specification<ReportView> getByStatus(ReportStatus status) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("status")).value(status);
	}

	public static Specification<ReportView> isBetweenDates(LocalDate startdate, LocalDate endDate) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("driveDate"), startdate, endDate);
	}

	public static Specification<ReportView> isBeforeDate(LocalDate date) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("driveDate"), date);
	}

	public static Specification<ReportView> isAfterDate(LocalDate date) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("driveDate"), date);
	}

	public static Specification<ReportView> getByName(String name) {
		return (root, query, criteriaBuilder) -> {
			if (name == null || name.isEmpty()) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), "%" + name.toLowerCase() + "%");
		};
	}
	
	public static Specification<ReportView> getById(long id) {
		return (root, query, criteriaBuilder) -> {
			if (Objects.equals(null, id) ||id == 0L) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.equal(root.get("personId"), id);
		};
	}
	
	public static Specification<ReportView> getByOrgUnit(String orgUnit) {
		return (root, query, criteriaBuilder) ->  {
			Predicate orgUnitInitialsPredicate = criteriaBuilder.like(root.get("orgunitIntials"), "%" + orgUnit + "%");
			Predicate orgUnitNamePredicate = criteriaBuilder.like(root.get("orgunitName"), "%" + orgUnit + "%");

			return criteriaBuilder.or(orgUnitInitialsPredicate, orgUnitNamePredicate);
		};
	}
}
