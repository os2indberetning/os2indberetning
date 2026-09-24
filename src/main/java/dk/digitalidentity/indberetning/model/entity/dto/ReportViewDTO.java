package dk.digitalidentity.indberetning.model.entity.dto;

import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportViewDTO {
    private long id;
    private ReportStatus status;
    private long personId;
    private LocalDateTime createdDate;
    private LocalDate driveDate;
    private String fullName;
    private long employeeNumber;
    private String approvedByName;
    private String potentialApprovers;
    private String purpose;
    private String kmRateType;
    private String comment;
    private double distance;
    private double amountToReimburse;
    private boolean extraDistance;
    private double extraDistanceAmount;
    private boolean fourKmRule;
    private String orgunitInitials;
    private String orgunitName;
    private String addresses;
    private String routeGeometry;
    private boolean fromApp;
    private boolean divergentAddress;
    private boolean roundTrip;
    private String userComment;
    private boolean startsAtHome;
    private boolean endsAtHome;
    private LocalDateTime processedDate;
    private LocalDateTime closedDate;
}

