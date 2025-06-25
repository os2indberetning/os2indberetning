package dk.digitalidentity.indberetning.controller.api;

import dk.digitalidentity.indberetning.exceptions.NotCommittedException;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.InternalAPIService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import dk.digitalidentity.indberetning.service.OrganisationService;
import dk.digitalidentity.indberetning.service.PersonService;
import dk.digitalidentity.indberetning.service.PersonalRouteService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class InternalAPIController {

	private final InternalAPIService internalAPIService;
	private final AddressService addressService;
	private final SubstituteService substituteService;
	private final ReportService reportService;
	private final PersonService personService;
	private final OrgUnitService orgUnitService;
	private final PersonalRouteService personalRouteService;
	private final LicensePlateService licensePlateService;
	private final OrganisationService organisationService;

	public record RecalculateRoutesRequestBody(boolean dryRun, List<Long> reportIds, List<Long> personIds, List<Long> employmentIds, boolean allowChangeNonPending, boolean requireReApprove, LocalDate driveDateStart, LocalDate driveDateEnd) {}
	@PostMapping("/internal/api/report/recalculate")
	public ResponseEntity<?> recalculateRoutes(@RequestBody RecalculateRoutesRequestBody requestBody) {
		List<Long> updatedReports = internalAPIService.recalculateReports(requestBody.dryRun, requestBody.reportIds, requestBody.personIds, requestBody.employmentIds,  requestBody.allowChangeNonPending, requestBody.requireReApprove, requestBody.driveDateStart, requestBody.driveDateEnd);
		return ResponseEntity.ok(updatedReports);
	}

	public record WashAddressRequest(boolean dryRun, AddressType addressType, String dirtyString, String streetName, String streetNumber, int zipCode, String town, double latitude, double longitude) {}
	@PostMapping("/internal/api/address/wash")
	public ResponseEntity<String> editDirtyAddress(@RequestBody WashAddressRequest washedAddressRequest) {
		List<Address> matchedAddresses = addressService.getByDirtyStringAndType(washedAddressRequest.dirtyString, washedAddressRequest.addressType);
		if (washedAddressRequest.dryRun) {
			return ResponseEntity.ok(Integer.toString(matchedAddresses.size()));
		}

		for (Address address : matchedAddresses) {
			if (!StringUtils.hasLength(address.getDirtyString())) {
				address.setDirtyString(address.getAddressString());
			}
			address.setStreetName(washedAddressRequest.streetName);
			address.setStreetNumber(washedAddressRequest.streetNumber);
			address.setZipCode(washedAddressRequest.zipCode);
			address.setTown(washedAddressRequest.town);
			address.setLatitude(washedAddressRequest.latitude);
			address.setLongitude(washedAddressRequest.longitude);
			addressService.save(address);
		}
		return ResponseEntity.ok(Integer.toString(matchedAddresses.size()));
	}

	public record EditAddressRequest(long id, String streetName, String streetNumber, int zipCode, String town, double latitude, double longitude, boolean dirty, String dirtyString, Long deviatingAddress) {}
	public record AddressDTO(long id, String streetName, String streetNumber, int zipCode, String town, double latitude, double longitude, String description, boolean dirty, String dirtyString, AddressType addressType, Long personId,
							 Long personalRouteId, Long orgUnitId, boolean standardAddress, boolean primary, LocalDateTime startDate, LocalDateTime endDate,
							 double homeToWorkDistanceOverrideDeviation, Long deviatingAddress, boolean dryRun, boolean createIfNotExists) {}
	@GetMapping("/internal/api/address")
	public ResponseEntity<?> getAddress(@RequestBody long id) {
		Address address = addressService.getById(id);
		if (address == null) {
			return ResponseEntity.badRequest().build();
		}
		Long personId = address.getPerson() == null ? null : address.getPerson().getId();
		Long personalRouteId = address.getPersonalRoute() == null ? null : address.getPersonalRoute().getId();
		Long orgUnitId = address.getOrgUnit() == null ? null : address.getOrgUnit().getId();
		Long deviatingAddressId = address.getDeviatingAddress() == null ? null : address.getDeviatingAddress().getId();
		AddressDTO result = new AddressDTO(address.getId(), address.getStreetName(), address.getStreetNumber(), address.getZipCode(), address.getTown(), address.getLatitude(), address.getLongitude(), address.getDescription(), address.isDirty(), address.getDirtyString(), address.getType(), personId, personalRouteId, orgUnitId, address.isStandardAddress(), address.isPrimary(), address.getStartDate(), address.getEndDate(), address.getHomeToWorkDistanceOverrideDeviation(), deviatingAddressId,true, false);
		return ResponseEntity.ok(result);
	}

	@Transactional
	@PostMapping("/internal/api/address")
	public ResponseEntity<?> editAddress(@RequestBody AddressDTO request) {
		Address address = new Address();
		if (!request.createIfNotExists) {
			address = addressService.getById(request.id);
			if (address == null) {
				return new ResponseEntity<>("The provided ID does not match an address our system", HttpStatus.BAD_REQUEST);
			}
		}
		String changelog = "\n";
		if (request.personId != null) {
			Person person = personService.getById(request.personId);
			if (person == null) {
				return new ResponseEntity<>("The provided ID does not match a person our system", HttpStatus.BAD_REQUEST);
			}
			if (!Objects.equals(address.getPerson(), person)) {
				String oldPerson = address.getPerson() == null ? "null" : address.getPerson().getName();
				changelog += "Person: " + oldPerson + " -> " + person.getName() + "\n";
				address.setPerson(person);
			}
		}
		else if (request.personId == null && address.getPerson() != null) {
			String oldPerson = address.getPerson().getName();
			changelog += "Person: " + oldPerson + " -> " + "null" + "\n";
			address.setPerson(null);
		}
		if (request.orgUnitId != null) {
			OrgUnit orgUnit = orgUnitService.findById(request.orgUnitId);
			if (orgUnit == null) {
				return new ResponseEntity<>("The provided ID does not match an OrgUnit our system", HttpStatus.BAD_REQUEST);
			}
			if (!Objects.equals(address.getOrgUnit(), orgUnit)) {
				String oldOrgUnit = address.getOrgUnit() == null ? "null" : address.getOrgUnit().getLongDescription();
				changelog += "OrgUnit: " + oldOrgUnit + " -> " + orgUnit.getLongDescription() + "\n";
				address.setOrgUnit(orgUnit);
			}
		}
		else if (request.orgUnitId == null && address.getOrgUnit() != null) {
			String oldOrgUnit = address.getOrgUnit().getLongDescription();
			changelog += "OrgUnit: " + oldOrgUnit + " -> " + "null" + "\n";
			address.setOrgUnit(null);
		}
		if (request.personalRouteId != null)  {
			PersonalRoute personalRoute = personalRouteService.findById(request.personalRouteId);
			if (personalRoute == null) {
				return new ResponseEntity<>("The provided ID does not match a personal route in our system", HttpStatus.BAD_REQUEST);
			}
			if (!Objects.equals(address.getPersonalRoute(), personalRoute)) {
				String oldPersonalRoute = address.getPersonalRoute() == null ? "null" : String.valueOf(address.getPersonalRoute().getId());
				changelog += "PersonalRoute: " + oldPersonalRoute + " -> " + personalRoute.getId() + "\n";
				address.setPersonalRoute(personalRoute);
			}
		}
		else if (request.personalRouteId == null && address.getPersonalRoute() != null) {
			String oldPersonalRoute = String.valueOf(address.getPersonalRoute().getId());
			changelog += "PersonalRoute: " + oldPersonalRoute + " -> " + "null" + "\n";
			address.setPersonalRoute(null);
		}
		if (!Objects.equals(request.primary, address.isPrimary())) {
			changelog += "Primary: " + address.isPrimary() + " -> " + request.primary + "\n";
			address.setPrimary(request.primary);
		}
		if (!Objects.equals(request.dirty, address.isDirty())) {
			changelog += "Dirty: " + address.isDirty() + " -> " + request.dirty + "\n";
			address.setDirty(request.dirty);
		}
		if (!Objects.equals(request.dirtyString, address.getDirtyString())) {
			changelog += "DirtyString: " + (address.getDirtyString() == null ? "null" : address.getDirtyString()) + " -> " + (request.dirtyString == null ? "null" : request.dirtyString) + "\n";
			address.setDirtyString(request.dirtyString);
		}
		if (!Objects.equals(request.addressType, address.getType())) {
			changelog += "AddressType: " + address.getType() + " -> " + request.addressType + "\n";
			address.setType(request.addressType);
		}
		if (!Objects.equals(request.deviatingAddress, address.getDeviatingAddress() == null ? null : address.getDeviatingAddress().getId())) {
			if (request.deviatingAddress != null) {
				Address deviatingAddress = addressService.getById(request.deviatingAddress);
				if (deviatingAddress == null) {
					return ResponseEntity.badRequest().body("Could not find deviating address");
				}
				String oldDeviation = address.getDeviatingAddress() == null ? "null" : String.valueOf(address.getDeviatingAddress().getId());
				changelog += "DeviatingAddress: " + oldDeviation + " -> " + deviatingAddress.getId() + "\n";
				address.setDeviatingAddress(deviatingAddress);
			}
			else {
				changelog += "DeviatingAddress: " + address.getId() + " -> " + "null" + "\n";
				address.setDeviatingAddress(null);
			}
		}
		if (!Objects.equals(request.description, address.getDescription())) {
			changelog += "Description: " + address.getDescription() + " -> " + request.description + "\n";
			address.setDescription(request.description);
		}
		if (!Objects.equals(request.standardAddress, address.isStandardAddress())) {
			changelog += "StandardAddress: " + address.isStandardAddress() + " -> " + request.standardAddress + "\n";
			address.setStandardAddress(request.standardAddress);
		}
		if (!Objects.equals(request.streetName, address.getStreetName())) {
			changelog += "Street: " + address.getStreetName() + " -> " + request.streetName + "\n";
			address.setStreetName(request.streetName);
		}
		if (!Objects.equals(request.streetNumber, address.getStreetNumber())) {
			changelog += "StreetNumber: " + address.getStreetNumber() + " -> " + request.streetNumber + "\n";
			address.setStreetNumber(request.streetNumber);
		}
		if (!Objects.equals(request.town, address.getTown())) {
			changelog += "Town: " + address.getTown() + " -> " + request.town + "\n";
			address.setTown(request.town);
		}
		if (!Objects.equals(request.startDate, address.getStartDate())) {
			changelog += "StartDate: " + address.getStartDate() + " -> " + request.startDate + "\n";
			address.setStartDate(request.startDate);
		}
		if (!Objects.equals(request.endDate, address.getEndDate())) {
			changelog += "EndDate: " + address.getEndDate() + " -> " + request.endDate + "\n";
			address.setEndDate(request.endDate);
		}
		if (!Objects.equals(request.latitude(), address.getLatitude())) {
			changelog += "Latitude: " + address.getLatitude() + " -> " + request.latitude + "\n";
			address.setLatitude(request.latitude);
		}
		if (!Objects.equals(request.longitude(), address.getLongitude())) {
			changelog += "Longitude: " + address.getLongitude() + " -> " + request.longitude + "\n";
			address.setLongitude(request.longitude);
		}
		if (!Objects.equals(request.homeToWorkDistanceOverrideDeviation(), address.getHomeToWorkDistanceOverrideDeviation())) {
			changelog += "HomeToWorkDistanceOverrideDeviation: " + address.getHomeToWorkDistanceOverrideDeviation() + " -> " + request.homeToWorkDistanceOverrideDeviation + "\n";
			address.setHomeToWorkDistanceOverrideDeviation(request.homeToWorkDistanceOverrideDeviation);
		}

		if (request.dryRun) {
			throw new NotCommittedException(changelog);
		}
		else {
			addressService.save(address);
			return new ResponseEntity<>("The following changes have been made to the address in DB: " + changelog, HttpStatus.OK);
		}
	}

	public record DeleteDeviatingAddressesRequest(long id) {}
	@Operation(
			summary = "Delete deviating address",
			description = "Deletes a deviating address from a person",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	@PostMapping("/internal/api/address/deviating/delete")
	public ResponseEntity<String> deleteDeviatingAddress(@RequestBody EditAddressRequest request) {
		// HAS to be of type deviating otherwise this might ruin calculations instead of just resetting bad data input
		Address address = addressService.getById(request.id);
		if (address == null) {
			return ResponseEntity.badRequest().build();
		}

		switch (address.getType()) {
			case HOME, WORK, ALTERNATIVE, STANDARD, PERSONAL_ROUTE_POINT -> {
				return ResponseEntity.unprocessableEntity().build();
			}
			case DHOME, DWORK -> {
				// OK
			}
			case null, default -> {
				throw new IllegalStateException("Unexpected value: " + address.getType());
			}
		}

		addressService.delete(request.id);

		return ResponseEntity.ok().build();
	}

	public record PersonResponse(long id) {}
	public record FindApproversRequest(long reportId) {}
	@Operation(
			summary = "Find all approvers",
			description = "Finds all approvers given a persons id",
			responses = {
					@ApiResponse(responseCode = "200", description = "Approvers found!"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	@PostMapping("/internal/api/findApprovers")
	public ResponseEntity<?> findApprovers(@RequestBody FindApproversRequest request) {
		Report report = reportService.getById(request.reportId);
		if (report == null) {
			return ResponseEntity.badRequest().build();
		}

		Set<Person> people = reportService.findApprovers(report);
		List<PersonResponse> result = people.stream()
				.map(employment -> new PersonResponse(employment.getId()))
				.sorted(Comparator.comparingLong(o -> o.id))
				.toList();

		return ResponseEntity.ok(result);
	}

	public record EmploymentResponse(long id, String employeeNumber, boolean leader) {}
	public record WhoDoILeadOrSubRequest(long personId, LocalDate date) {}
	@PostMapping("/internal/api/whoDoILeadOrSub")
	@Operation(
			summary = "Find approvable persons",
			description = "Find all persons this person can approve",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> findWhoCanBeApproved(@RequestBody WhoDoILeadOrSubRequest request) {
		Person person = personService.getById(request.personId);
		if (person == null) {
			return ResponseEntity.badRequest().build();
		}

		LocalDate date = request.date;
		if (request.date == null) {
			date = LocalDate.now();
		}

		Set<Employment> employments = substituteService.whoDoILeadOrSubNotCached(person, date);
		List<EmploymentResponse> result = employments.stream()
				.map(employment -> new EmploymentResponse(employment.getId(), employment.getEmployeeNumber(), employment.isLeader()))
				.sorted(Comparator.comparingLong(o -> o.id))
				.toList();
		return ResponseEntity.ok(result);
	}

	public record APIReport(ReportStatus reportStatus, LocalDate driveDate, double rawDistance, boolean roundTrip,
							String kmRateType, double kmRate, boolean fourKmRule, CalculationType calculationType,
							double distance, boolean notificationSent, Integer payType, Integer sequentialNumber,
							String overrideCostCenter, String overridePspElement, boolean recalculate, boolean dryRun) {}
	@GetMapping("/internal/api/report/{id}")
	@Operation(
			summary = "Get report by id",
			description = "Get report found by id",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> getReport(@PathVariable long id) {
		Report report = reportService.getById(id);
		if (report == null) {
			return new ResponseEntity<>("Report not found on id " + id, HttpStatus.BAD_REQUEST);
		}
		else {
			APIReport reportResponse = new APIReport(report.getStatus(), report.getDriveDate(), report.getRawDistance(), report.isRoundTrip(), report.getKmRateType(), report.getKmRate(), report.isFourKmRule(), report.getCalculationType(), report.getDistance(), report.isNotificationSent(), report.getPayType(), report.getSequentialNumber(), report.getOverrideCostCenter(), report.getOverridePspElement(), report.isRecalculate(), true);
			return new ResponseEntity<>(reportResponse, HttpStatus.OK);
		}
	}

	@Transactional
	@PostMapping("/internal/api/report/{id}")
	@Operation(
			summary = "Edit report",
			description = "Edit report given an id and reasonable data (With dry-run setting)",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> editReport(@PathVariable long id, @RequestBody APIReport requestedChanges) {
		Report report = reportService.getById(id);
		if (report == null) {
			return new ResponseEntity<>("Report not found on id " + id, HttpStatus.BAD_REQUEST);
		}
		if (requestedChanges == null) {
			return new ResponseEntity<>("The provided body is empty", HttpStatus.BAD_REQUEST);
		}
		else {
			if (requestedChanges.reportStatus == null || requestedChanges.driveDate == null || requestedChanges.kmRateType == null || requestedChanges. calculationType == null || requestedChanges.payType == null || requestedChanges.sequentialNumber == null) {
				return new ResponseEntity<>("The provided body includes null values!", HttpStatus.BAD_REQUEST);
			}
			String changelog = "\n";
			if (report.getStatus() != requestedChanges.reportStatus) {
				changelog += "Status: " + report.getStatus() + " -> " + requestedChanges.reportStatus + "\n";
				report.setStatus(requestedChanges.reportStatus);
			}
			if (!Objects.equals(report.getDriveDate(), requestedChanges.driveDate)) {
				changelog += "DriveDate: " + report.getDriveDate() + " -> " + requestedChanges.driveDate + "\n";
				report.setDriveDate(requestedChanges.driveDate);
			}
			if (report.getRawDistance() != requestedChanges.rawDistance) {
				changelog += "RawDistance: " + report.getRawDistance() + " -> " + requestedChanges.rawDistance + "\n";
				report.setRawDistance(requestedChanges.rawDistance);
			}
			if (report.isRoundTrip() != requestedChanges.roundTrip) {
				changelog += "RoundTrip: " + report.isRoundTrip() + " -> " + requestedChanges.roundTrip + "\n";
				report.setRoundTrip(requestedChanges.roundTrip);
			}
			if (!report.getKmRateType().equals(requestedChanges.kmRateType)) {
				changelog += "KmRateType: " + report.getKmRateType() + " -> " + requestedChanges.kmRateType + "\n";
				report.setKmRateType(requestedChanges.kmRateType);
			}
			if (report.getKmRate() != requestedChanges.kmRate) {
				changelog += "KmRate: " + report.getKmRate() + " -> " + requestedChanges.kmRate + "\n";
				report.setKmRate(requestedChanges.kmRate);
			}
			if (report.isFourKmRule() != requestedChanges.fourKmRule) {
				changelog += "4KMRule: " + report.isFourKmRule() + " -> " + requestedChanges.fourKmRule + "\n";
				report.setFourKmRule(requestedChanges.fourKmRule);
			}
			if (report.getCalculationType() != requestedChanges.calculationType) {
				changelog += "CalculationType: " + report.getCalculationType() + " -> " + requestedChanges.calculationType + "\n";
				report.setCalculationType(requestedChanges.calculationType);
			}
			if (report.getDistance() != requestedChanges.distance) {
				changelog += "Distance: " + report.getDistance() + " -> " + requestedChanges.distance + "\n";
				report.setDistance(requestedChanges.distance);
			}
			if (report.isNotificationSent() != requestedChanges.notificationSent) {
				changelog += "NotificationSent: " + report.isNotificationSent() + " -> " + requestedChanges.notificationSent + "\n";
				report.setNotificationSent(requestedChanges.notificationSent);
			}
			if (!Objects.equals(report.getPayType(), requestedChanges.payType)) {
				changelog += "PayType: " + report.getPayType() + " -> " + requestedChanges.payType + "\n";
				report.setPayType(requestedChanges.payType);
			}
			if (!Objects.equals(report.getSequentialNumber(), requestedChanges.sequentialNumber)) {
				changelog += "SequentialNumber: " + report.getSequentialNumber() + " -> " + requestedChanges.sequentialNumber + "\n";
				report.setSequentialNumber(requestedChanges.sequentialNumber);
			}
			if (!Objects.equals(report.getOverrideCostCenter(), requestedChanges.overrideCostCenter)) {
				changelog += "OverrideCostCenter: " + report.getOverrideCostCenter() + " -> " + requestedChanges.overrideCostCenter + "\n";
				report.setOverrideCostCenter(requestedChanges.overrideCostCenter);
			}
			if (!Objects.equals(report.getOverridePspElement(), requestedChanges.overridePspElement)) {
				changelog += "OverridePspElement: " + report.getOverridePspElement() + " -> " + requestedChanges.overridePspElement + "\n";
				report.setOverridePspElement(requestedChanges.overridePspElement);
			}
			if (report.isRecalculate() != requestedChanges.recalculate) {
				changelog += "Recalculate: " + report.isRecalculate() + " -> " + requestedChanges.recalculate + "\n";
				report.setRecalculate(requestedChanges.recalculate);
			}
			if (requestedChanges.dryRun) {
				throw new NotCommittedException(changelog);
			}
			else {
				reportService.save(report);
				return new ResponseEntity<>("The following changes have been made to the report in DB: " + changelog, HttpStatus.OK);
			}
		}
	}

	@PostMapping("/internal/api/watchList/add")
	@Operation(
			summary = "Used to add a person to the watchList",
			description = "Used to add a person to the watchList",
			responses = {
					@ApiResponse(responseCode = "200", description = "Person opdateret!"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> putOnWatchList(@RequestBody List<Long> ids) {
		List<Long> persons = new ArrayList<>();
		for (Long id : ids) {
			Person person = personService.getById(id);
			if (person == null) {
				return new ResponseEntity<>("The person with id " + id + " could not be found!", HttpStatus.BAD_REQUEST);
			}
			persons.add(person.getId());
		}
		organisationService.setWatchList(persons);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/internal/api/watchList/get")
	@Operation(
			summary = "Used to add a person to the watchList",
			description = "Used to add a person to the watchList",
			responses = {
					@ApiResponse(responseCode = "200", description = "Person opdateret!"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "400", description = "Could not delete deviating address")
			}
	)
	public ResponseEntity<?> getList() {
		List<Long> watchList = organisationService.getWatchList();
		return new ResponseEntity<>(watchList, HttpStatus.OK);
	}

	@PostMapping("/internal/api/statistics/reset")
	@Operation(
			summary = "Reset statistics",
			description = "Reset statistics on calls to Septima",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized")
			}
	)
	public ResponseEntity<?> resetStatistics() {
		internalAPIService.resetStatistics();
		return ResponseEntity.ok("Statistics reset");
	}

	public record LicensePlateDTO(long id, String registrationNumber, String description, long personId, boolean prime, boolean dryRun) {}


	@GetMapping("/internal/api/licenseplate/{id}")
	@Operation(
			summary = "Fetches a licenseplate of given ID",
			description = "Fetches a licenseplate of given ID",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized")
			}
	)
	public ResponseEntity<?> getLicensePlate(@PathVariable Long id) {
		LicensePlate licensePlate = licensePlateService.getById(id);
		if (licensePlate == null) {
			return new ResponseEntity<>("Licenseplate not found on id " + id, HttpStatus.BAD_REQUEST);
		}
		LicensePlateDTO result = new LicensePlateDTO(licensePlate.getId(), licensePlate.getRegistrationNumber(), licensePlate.getDescription(), licensePlate.getPerson().getId(), licensePlate.isPrime(), true);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@Transactional
	@PostMapping("/internal/api/licenseplate/{id}")
	@Operation(
			summary = "Edits a licenseplate of given ID",
			description = "Edits a licenseplate of given ID",
			responses = {
					@ApiResponse(responseCode = "200", description = ""),
					@ApiResponse(responseCode = "401", description = "Unauthorized")
			}
	)
	public ResponseEntity<?> editLicensePlate(@PathVariable Long id, @RequestBody LicensePlateDTO request) {
		LicensePlate licensePlate = licensePlateService.getById(id);
		if (licensePlate == null) {
			return new ResponseEntity<>("Licenseplate not found on id " + id, HttpStatus.BAD_REQUEST);
		}
		String changelog = "\n";
		if (licensePlate.isPrime() != request.prime) {
			changelog += "Prime: " + licensePlate.isPrime() + " -> " + request.prime + "\n";
			licensePlate.setPrime(request.prime);
		}
		if (licensePlate.getRegistrationNumber() != request.registrationNumber) {
			changelog += "RegistrationNumber: " + licensePlate.getRegistrationNumber() + " -> " + request.registrationNumber + "\n";
			licensePlate.setRegistrationNumber(request.registrationNumber);
		}
		if (licensePlate.getDescription() != request.description) {
			changelog += "Description: " + licensePlate.getDescription() + " -> " + request.description + "\n";
			licensePlate.setDescription(request.description);
		}
		Person person = personService.getById(request.id);
		if (person == null) {
			return new ResponseEntity<>("Person not found on id " + id + " when trying to edit, aborting...", HttpStatus.BAD_REQUEST);
		}
		if (licensePlate.getPerson() != person) {
			changelog += "Prime: " + licensePlate.isPrime() + " -> " + request.prime + "\n";
			licensePlate.setPerson(person);
		}
		if (request.dryRun) {
			throw new NotCommittedException(changelog);
		}
		else {
			licensePlateService.save(licensePlate);
			return new ResponseEntity<>("The following changes have been made to the licenseplate in DB: " + changelog, HttpStatus.OK);
		}
	}
}
