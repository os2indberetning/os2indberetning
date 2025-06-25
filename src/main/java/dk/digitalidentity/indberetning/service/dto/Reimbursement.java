package dk.digitalidentity.indberetning.service.dto;

import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
@Setter
@AllArgsConstructor
public class Reimbursement {
	double amountToReimburse;
	List<Report> reports;
}