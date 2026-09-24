package dk.digitalidentity.indberetning.controller.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.indberetning.exceptions.UnprocessableContentException;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.AuditLogService;
import dk.digitalidentity.indberetning.service.OrganisationService;
import dk.digitalidentity.indberetning.service.OrganisationService.LogPersonRecord;
import dk.digitalidentity.indberetning.service.OrganisationService.UpdateOrgRecord;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.dto.AddressDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class OrganisationAPIController {
	private final AuditLogService auditLogService;
	private final OrganisationService organisationService;
	private final OrgUnitService orgUnitService;
	private final PersonService personService;

	public record EmploymentDTO(String employeeNumber, LocalDate fromDate, LocalDate toDate, String orgUnitId, String position, long costCenter, boolean manager, int extraNumber, int employmentType, String instituteCode) {}
	public record PersonDTO(String cpr, String firstName, String lastName, AddressDTO address, String email, List<EmploymentDTO> employments, String initials) {}
	public record OrgUnitDTO(String id, String parentId, String name, AddressDTO address, String costCenter) {}
	public record OrganizationDTO(List<OrgUnitDTO> orgUnits, List<PersonDTO> persons) {}

	@PostMapping("/api/UpdateOrganization")
	@Operation(
			summary = "Used to read data into the system",
			description = "Used to load data into the system",
			responses = {
					@ApiResponse(responseCode = "200", description = "Organisation opdateret!"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> updateOrganization(@RequestBody OrganizationDTO organizationDTO) {
		try {
			StopWatch watch = new StopWatch();
			watch.start();

			List<OrgUnit> orgUnits = orgUnitService.getAllWithAddresses();

			Map<String, OrgUnit> dbOuMap = organisationService.updateOrgUnits(organizationDTO.orgUnits(), orgUnits);

			// Update persons
			LogPersonRecord personsUpdateRecord = organisationService.updatePersons(organizationDTO.persons(), dbOuMap);

			organisationService.setWatchList(new ArrayList<>());

			watch.stop();			
			double totalTimeSeconds = watch.getTotalTimeSeconds();

			log.info("update organisation took " + totalTimeSeconds + " seconds.");
			auditLogService.saveSystem(LogAction.API_UPDATE_ORG, "Organisation og personer indlæst", new UpdateOrgRecord(Double.toString(totalTimeSeconds), personsUpdateRecord.peopleUpdated(), personsUpdateRecord.employmentsUpdated(), personsUpdateRecord.addressesUpdated()));

			return ResponseEntity.ok().build();
		}
		catch (UnprocessableContentException e) {
			return ResponseEntity.unprocessableEntity().body(e.getMessage());
		}
	}

	@GetMapping("/api/GetReportsToPayroll")
	public void getReportsToPayroll() {
		//TODO: ADD LOGGING ONCE IMPLEMENTED
		throw new NotImplementedException();
	}

	@PostMapping("/api/AcknowledgeReportsProcessed")
	public void acknowledgeReportsProcessed() {
		//TODO: ADD LOGGING ONCE IMPLEMENTED
		throw new NotImplementedException();
	}

	@PostMapping("/api/CalculateDrivenDistances")
	public void calculateDrivenDistances() {
		//TODO: ADD LOGGING ONCE IMPLEMENTED
		throw new NotImplementedException();
	}

	@PostMapping("/api/StitchHistoricTrips")
	public void stitchHistoricTrips() {
		throw new NotImplementedException();
	}
	
}
