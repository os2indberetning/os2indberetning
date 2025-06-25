 package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@ToString
@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {
    //TODO: Clean up unecessary variables and section this code

    // The following fields are set when creating/editing a report
    // and will then be used to calculate the reimbursement later on
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "edited_date")
    private LocalDateTime editedDate;

    @JsonFormat(pattern="yyyy-MM-dd")
    @Column(name = "drive_date", nullable = false)
    private LocalDate driveDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employment_id", nullable = false)
    private Employment employment;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "raw_distance")
    private double rawDistance;

    @Column(name = "is_round_trip")
    private boolean roundTrip;

    @Column(name = "km_rate_type")
    private String kmRateType;

    @Column(name = "km_rate")
    private double kmRate;

    @Column(name = "four_km_rule")
    private boolean fourKmRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type")
    private CalculationType calculationType;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "is_from_app", nullable = false)
    private boolean fromApp;

    @Column(name = "app_uuid")
    private String appUuid;

    // TODO: better name, this is used when using calculation type read to provide aditional info about the route.
    @Column(name = "comment")
    private String comment;

    //this fetches route, which is a separate table just for routeGeometry.
    //fetches lazy, so the db does have to write the very long encoded polyline on each select
    @OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @OneToMany(mappedBy = "report")
    private List<GpsCoordinate> coords = new LinkedList<>();

    // The following fields are set when calculating reimbursement for an existing report
    @Column(name = "distance")
    private double distance;

    @Column(name = "amount_to_reimburse")
    private double amountToReimburse;

    @Column(name = "home_to_border_distance")
    private double homeToBorderDistance;

    @Column(name = "starts_at_home")
    private boolean startsAtHome;

    @Column(name = "ends_at_home")
    private boolean endsAtHome;

    @Column(name = "is_extra_distance")
    private boolean extraDistance;

    @Column(name = "notification_sent")
    private boolean notificationSent;

    // The following fields are set when approving and processing an existing report
    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private Person approvedBy;

    @Column(name = "potential_approvers")
    private String potentialApprovers;

    @ManyToOne
    @JoinColumn(name = "actual_leader_id")
    private Person actualLeader;

    @Column(name = "pay_type")
    private Integer payType;

    @Column(name = "sequential_number")
    private Integer sequentialNumber;

    @Column(name = "override_cost_center")
    private String overrideCostCenter;

    @Column(name = "override_psp_Element")
    private String overridePspElement;

    @Column(name = "user_comment")
    private String userComment;

    @Column(name = "sixty_days_rule")
    private boolean sixtyDaysRule;

    @Column(name = "is_divergent_address")
    private boolean divergentAddress;

    @Column(name = "active_year")
    private int activeYear;

    @Column(name = "flagged_sixty_days")
    private boolean flaggedSixtyDayRule;

    @Column(name = "recalculate")
    private boolean recalculate;

    public String getAddressesString() {
        StringBuilder b = new StringBuilder();
        if (coords != null && !coords.isEmpty()) {
            if (roundTrip) {
                b.append("Tur + retur: ").append('\n');
            }
            coords.forEach(gps -> b.append(gps.getAddress()).append('\n'));
        }
        return b.toString();
    }

    public GpsCoordinate getStartGps() {
        return coords.stream().filter(GpsCoordinate::isStartPoint).findAny().orElse(!coords.isEmpty() ? coords.getFirst() : null);
    }

    public GpsCoordinate getEndGps() {
        return coords.stream().filter(GpsCoordinate::isEndPoint).findAny().orElse(!coords.isEmpty() ? coords.getLast() : null);
    }
}