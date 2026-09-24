package dk.digitalidentity.indberetning.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.controller.api.OrganisationAPIController;
import dk.digitalidentity.indberetning.exceptions.UnprocessableContentException;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.service.dto.AddressDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrganisationService {
	private final OS2indberetningConfiguration configuration;
	private final OrgUnitService orgUnitService;
	private final PersonService personService;
	private final EmploymentService employmentService;
	private final AddressService addressService;
	private final ApiTimeStampService apiTimeStampService;

	@Getter
	@Setter
	private List<Long> watchList = new ArrayList<>();

	public record LogPersonRecord(int peopleUpdated, int employmentsUpdated, int addressesUpdated) {}
	public record UpdateOrgRecord(String elapsedTime, int peopleUpdated, int employmentsUpdated, int addressesUpdated) {}

	@Transactional(readOnly = true)
	public LogPersonRecord updatePersons(List<OrganisationAPIController.PersonDTO> dto, Map<String, OrgUnit> dbOuMap) {
		log.info("updatePersons - entry");

		List<Person> existingPersons = personService.getAllWithEmploymentsAndAddresses();
		Map<String, Person> dbPersons = existingPersons.stream().collect(Collectors.toMap(Person::getCpr, Function.identity()));
		Map<String, OrganisationAPIController.PersonDTO> dtoPersons = dto.stream().collect(Collectors.toMap(personDTO -> personDTO.cpr(), Function.identity()));
		long updateCount = 0, createCount = 0, deleteCount = 0;

		log.info("updatePersons - existingPersons " + existingPersons.size());

		// Create / Update
		ArrayList<Person> toBeSaved = new ArrayList<>();
		ArrayList<Employment> employmentsToBeSaved = new ArrayList<>();
		ArrayList<Address> addressesToBeSaved = new ArrayList<>();

		for (OrganisationAPIController.PersonDTO personDTO : dto) {
			Person person = dbPersons.get(personDTO.cpr());
			if (person != null) {
				boolean updated = updatePerson(person, personDTO, employmentsToBeSaved, dbOuMap, addressesToBeSaved);
				if (updated) {
					updateCount++;
					toBeSaved.add(person);
				}
			}
			else {
				createCount++;
				toBeSaved.add(createPerson(personDTO, employmentsToBeSaved, dbOuMap, addressesToBeSaved));
			}
		}

		log.info("updatePerson - done perform update/create");

		// Delete
		for (Person person : existingPersons) {
			// ignore already deleted persons :)
			if (!person.isActive()) {
				continue;
			}
			
			if (!dtoPersons.containsKey(person.getCpr())) {
				deleteCount++;
				person.setActive(false);
				toBeSaved.add(person);

				person.getEmployments().stream()
						.filter(employment -> employment.getStopDate() == null || employment.getStopDate().isAfter(LocalDateTime.now()))
						.forEach(employment -> employment.setStopDate(LocalDateTime.now()));
			}
		}
		
		log.info("updatePerson - done perform delete");
		
		log.info("updatePerson - createCount = " + createCount + ", updateCount=" + updateCount + ", deleteCount=" + deleteCount);

		log.info("updatePerson - personsToBeSaved = " + toBeSaved.size());
		List<Person> saved = personService.saveAllInIsolatedTransaction(toBeSaved);

		log.info("updatePerson - addressesToBeSaved = " + addressesToBeSaved.size());
		List<Address> addr = addressService.saveAllInIsolatedTransaction(addressesToBeSaved);

		log.info("updatePerson - employmentsToBeSaved = " + employmentsToBeSaved.size());
		List<Employment> emp = employmentService.saveAllInIsolatedTransaction(employmentsToBeSaved);

		apiTimeStampService.setLastUpdated();

		return new LogPersonRecord(toBeSaved.size(), emp.size(), addressesToBeSaved.size());
	}

	private Person createPerson(OrganisationAPIController.PersonDTO dto, ArrayList<Employment> employmentsToBeSaved, Map<String, OrgUnit> orgUnits, ArrayList<Address> addressesToBeSaved) {
		Person person = new Person();
		person.setCpr(dto.cpr()); // Is this not always the same? this is what we match on!
		person.setFirstName(dto.firstName());
		person.setLastName(dto.lastName());
		person.setEmail(dto.email()); // Email is not necessarily supplied from DTO, sometimes we get it from SAML login, so no overwriting with empty value

		// These are only set on creation and otherwise controlled from the UI,
		// SAML login will flip the Admin flag, but we need to keep track of it for email sending.
		person.setAdmin(false);
		person.setReceiveEmail(true);

		person.setActive(false); // We flip this to true if the person has a current or future employment
		
		//default all to no email notifications until we have an email on the person then we flip these.
		person.setReceivePersonalMail(false);
		person.setReceiveAdminMail(false);
		person.setReceiveApproverMail(false);

		// Update employments
		employmentsToBeSaved.addAll(updateEmployments(person, dto, orgUnits));

		// Create home address
		Address address = new Address();

		AddressDTO addressDTO = dto.address();
		address.setStreetName(addressDTO.getStreetName());
		address.setStreetNumber(addressDTO.getStreetNumber());
		address.setZipCode(addressDTO.getPostalCode());
		address.setTown(addressDTO.getCity());
		address.setType(AddressType.HOME);
		address.setPerson(person);


//		try {
//			routeService.populateAddressLatLng(address);
//		}
//		catch (JsonProcessingException e) {
//			throw new RuntimeException(e);
//		}

		addressesToBeSaved.add(address);


		return person;
	}

	private boolean updatePerson(Person person, OrganisationAPIController.PersonDTO dto, ArrayList<Employment> employmentsToBeSaved, Map<String, OrgUnit> orgUnits, ArrayList<Address> addressesToBeSaved) {
		boolean updated = false;

		// Update from DTO
		if (!Objects.equals(person.getFirstName(), dto.firstName())) {
			if (watchList.contains(person.getId())) {
				log.info("FirstName: " + person.getFirstName() + " -> " + dto.firstName());
			}
			updated = true;
			person.setFirstName(dto.firstName());
		}

		if (!Objects.equals(person.getLastName(), dto.lastName())) {
			if (watchList.contains(person.getId())) {
				log.info("LastName: " + person.getLastName() + " -> " + dto.lastName());
			}
			updated = true;
			person.setLastName(dto.lastName());
		}

		// Email is not necessarily supplied from DTO, sometimes we get it from SAML login, so no overwriting with empty value
		if (StringUtils.hasLength(dto.email()) && !Objects.equals(person.getEmail(), dto.email())) {
			if (watchList.contains(person.getId())) {
				log.info("Email: " + person.getEmail() + " -> " + dto.email());
			}
			updated = true;
			person.setEmail(dto.email());
		}

		employmentsToBeSaved.addAll(updateEmployments(person, dto, orgUnits));

		LocalDateTime now = LocalDateTime.now();
		Address currentAddress = addressService.getHomeAddressByDate(person, now);		
		AddressDTO addressDTO = dto.address();

		if (isAddressChanged(currentAddress, addressDTO)) {
			if (watchList.contains(person.getId())) {
				log.info("The address of: " + person.getName() + " has changed!");
			}

			// Address has changed!
			if (currentAddress != null) {
				if (watchList.contains(person.getId())) {
					log.info("OldAddress: " + currentAddress.getAddressString() + " end date: " + " -> " + now);
				}
				currentAddress.setEndDate(now);
				addressesToBeSaved.add(currentAddress);

				Address deviatingAddress = currentAddress.getDeviatingAddress();
				if (deviatingAddress != null) {
					if (watchList.contains(person.getId())) {
						log.info("Deviating Address: " + deviatingAddress.getAddressString() + " end date: " + " -> " + now);
					}
					deviatingAddress.setEndDate(now);
					addressesToBeSaved.add(deviatingAddress);
				}
			}

			Address address = new Address();
			address.setStreetName(addressDTO.getStreetName());
			address.setStreetNumber(addressDTO.getStreetNumber());
			address.setZipCode(addressDTO.getPostalCode());
			address.setTown(addressDTO.getCity());
			address.setStartDate(now);

//			try {
//				routeService.populateAddressLatLng(address);
//			}
//			catch (JsonProcessingException e) {
//				throw new RuntimeException(e);
//			}

			address.setType(AddressType.HOME);
			address.setPerson(person);
			addressesToBeSaved.add(address);

			if (watchList.contains(person.getId())) {
				log.info("NewAddress: " + address.getAddressString() + " start date: " + now);
			}
		}

		return updated;
	}

	private static boolean isAddressChanged(Address currentAddress, AddressDTO addressDTO) {
		boolean addressChanged = (currentAddress == null);

		if (!addressChanged) {
			Address deviatingAddress = currentAddress.getDeviatingAddress();
			if (deviatingAddress != null) {
				// Address is deviating check against the current actual address
				currentAddress = deviatingAddress;
			}

			if (currentAddress.isDirty() && StringUtils.hasLength(currentAddress.getDirtyString())) {
				addressChanged = !Objects.equals(currentAddress.getDirtyString(), addressDTO.getStreetName() + " " + addressDTO.getStreetNumber() + ", " + addressDTO.getPostalCode() + " " + addressDTO.getCity());
			}
			else {
				if (StringUtils.hasLength(addressDTO.getCity()) && (!StringUtils.hasLength(currentAddress.getTown()) || !Objects.equals(addressDTO.getCity(), currentAddress.getTown()))) {
					addressChanged = true;
				}

				if (!Objects.equals(currentAddress.getZipCode(), addressDTO.getPostalCode())) {
					addressChanged = true;
				}

				if (!Objects.equals(currentAddress.getStreetName(), addressDTO.getStreetName())) {
					addressChanged = true;
				}

				if (!Objects.equals(currentAddress.getStreetNumber(), addressDTO.getStreetNumber())) {
					addressChanged = true;
				}
			}
		}

		if (addressChanged) {
			log.trace(addressDTO.toString());
		}

		return addressChanged;
	}

	private List<Employment> updateEmployments(Person person, OrganisationAPIController.PersonDTO dto, Map<String, OrgUnit> orgUnits) {
		List<Employment> toBeUpdated = new ArrayList<>();
		List<Employment> existingEmployments = person.getEmployments();
		Map<String, Employment> existingEmploymentMap = existingEmployments.stream().collect(Collectors.toMap(Employment::getEmployeeNumber, Function.identity()));

		// "Delete" employments
		if (existingEmployments != null && !existingEmployments.isEmpty()) {
			for (Employment existingEmployment : existingEmployments) {
				if (existingEmployment.getStopDate() != null && !existingEmployment.getStopDate().isAfter(LocalDateTime.now())) {
					continue; // We ignore already finished employments, no need to change them.
				}

				boolean matchingDTO =  dto.employments() != null && dto.employments().stream().anyMatch(employmentDTO -> Objects.equals(employmentDTO.employeeNumber(), existingEmployment.getEmployeeNumber()));
				if (!matchingDTO) {
					existingEmployment.setStopDate(LocalDateTime.now());
					log.info("Setting stopDate to today on employment: " + existingEmployment.getId());
					toBeUpdated.add(existingEmployment);
				}
			}
		}


		// Create/Update employments
		if (dto.employments() != null) {
			AtomicInteger unknownOUs = new AtomicInteger();
			ArrayList<OrganisationAPIController.EmploymentDTO> filteredEmployments = dto.employments().stream().filter(employmentDTO -> {
				if (!StringUtils.hasText(employmentDTO.orgUnitId()) || "0".equals(employmentDTO.orgUnitId())) {
					log.error("Employment {} has empty or invalid orgUnitId: {}", employmentDTO.employeeNumber(), employmentDTO.orgUnitId());
					unknownOUs.getAndIncrement();
					return false;
				}
				if (!orgUnits.containsKey(employmentDTO.orgUnitId())) {
					log.error("Employment {} references non-existent OU: {}", employmentDTO.employeeNumber(), employmentDTO.orgUnitId());
					unknownOUs.getAndIncrement();
					return false;
				}
				return true;
			}).collect(Collectors.toCollection(ArrayList::new));

			int maxUnknownOUs = configuration.getApi().getMaxUnknownOUs();
			if (unknownOUs.get() > maxUnknownOUs) {
				throw new RuntimeException("Too many unknown OUs aborting: " + unknownOUs.get() + " (max: " + maxUnknownOUs + ")");
			}

			for (OrganisationAPIController.EmploymentDTO employmentDTO : filteredEmployments) {


				boolean changed = false;
				Employment employment = existingEmploymentMap.get(employmentDTO.employeeNumber());

				if (employment == null) {
					changed = true;
					employment = new Employment();
					employment.setPerson(person);
					if (watchList.contains(person.getId())) {
						log.info("Person: " + person.getName() + " has a new employment");
					}
				}

				String orgId = employment.getOrgUnit() != null ? employment.getOrgUnit().getOrgId() : null;
				if (!Objects.equals(orgId, employmentDTO.orgUnitId())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						String ouName = employment.getOrgUnit() != null ? employment.getOrgUnit().getLongDescription() : "null";
						log.info("Employment OU: " + ouName + " -> " + orgUnits.get(employmentDTO.orgUnitId()).getLongDescription());
					}
					// OU existence is validated in the filter above, so this is guaranteed to exist
					employment.setOrgUnit(orgUnits.get(employmentDTO.orgUnitId()));
				}

				String position = StringUtils.hasLength(employmentDTO.position()) ? employmentDTO.position() : "Ingen stillingsbetegnelse";
				if (!Objects.equals(employment.getPosition(), position)) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Position: " + employment.getPosition() + " -> " + employmentDTO.position());
					}
					employment.setPosition(position);
				}

				if (!Objects.equals(employment.isLeader(), employmentDTO.manager())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Leader: " + employment.isLeader() + " -> " + employmentDTO.manager());
					}
					employment.setLeader(employmentDTO.manager());
				}

				LocalDateTime startDate = employmentDTO.fromDate() != null ? employmentDTO.fromDate().atTime(0,0) : null;
				if (!Objects.equals(employment.getStartDate(), startDate)) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Start Date: " + employment.getStartDate() + " -> " + employmentDTO.fromDate());
					}
					employment.setStartDate(startDate);
				}

				LocalDateTime stopDate = employmentDTO.toDate() != null ? employmentDTO.toDate().plusDays(1).atTime(0,0) : null;
				if (!Objects.equals(employment.getStopDate(), stopDate)) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Stop Date: " + employment.getStopDate() + " -> " + stopDate);
					}
					employment.setStopDate(stopDate);
				}

				if (!Objects.equals(employment.getExtraNumber(), employmentDTO.extraNumber())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Extra Number: " + employment.getExtraNumber() + " -> " + employmentDTO.extraNumber());
					}
					employment.setExtraNumber(employmentDTO.extraNumber());
				}

				if (!Objects.equals(employment.getCostCenter(), employmentDTO.costCenter())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Cost Center: " + employment.getCostCenter() + " -> " + employmentDTO.costCenter());
					}
					employment.setCostCenter(employmentDTO.costCenter());
				}

				if (!Objects.equals(employment.getEmployeeNumber(), employmentDTO.employeeNumber())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Employee Number: " + employment.getEmployeeNumber() + " -> " + employmentDTO.employeeNumber());
					}
					employment.setEmployeeNumber(employmentDTO.employeeNumber());
				}

				if (!Objects.equals(employment.getInstitutionCode(), employmentDTO.instituteCode())) {
					changed = true;
					if (watchList.contains(person.getId())) {
						log.info("Employment Institution Code: " + employment.getInstitutionCode() + " -> " + employmentDTO.instituteCode());
					}
					employment.setInstitutionCode(employmentDTO.instituteCode());
				}

				// Not happy with this logic but this is how the old solution works
				if (employment.getStopDate() == null || employment.getStopDate().isAfter(LocalDateTime.now())) {
					if (watchList.contains(person.getId())) {
						log.info("Person: " + person.getName() + " set to active!");
					}
					person.setActive(true);
				}

				if (changed) {
					toBeUpdated.add(employment);
				}
			}
		}

		return toBeUpdated;
	}

	// TODO: should recode this to only be transactional on the parts that need it
	@Transactional
	public Map<String, OrgUnit> updateOrgUnits(List<OrganisationAPIController.OrgUnitDTO> orgUnitDTOS, List<OrgUnit> orgUnits) throws UnprocessableContentException {
		Map<String, OrgUnit> dbOuMap = orgUnits.stream().collect(Collectors.toMap(OrgUnit::getOrgId, Function.identity()));

		log.info("orgUnits from DTOs " + orgUnitDTOS.size());
		log.info("orgUnits from database " + orgUnits.size());

		List<OrganisationAPIController.OrgUnitDTO> rootOUs = orgUnitDTOS.stream()
				.filter(orgUnitDTO -> !StringUtils.hasLength(orgUnitDTO.parentId()))
				.collect(Collectors.toList());

		if (rootOUs.isEmpty()) {
			throw new UnprocessableContentException("No root OU supplied");
		}
		else if (rootOUs.size() > 1) {
			throw new UnprocessableContentException("Multiple OU roots supplied");
		}

		ArrayList<OrgUnit> toBeSaved = new ArrayList<>();
		OrganisationAPIController.OrgUnitDTO rootDTO = rootOUs.get(0);
		OrgUnit rootOU = dbOuMap.get(rootDTO.id());

		ArrayList<Address> addressesToBeSaved = new ArrayList<>();

		if (rootOU == null) {
			// Generate new root ou, no matching ou in list
			rootOU = createOU(rootDTO, null, addressesToBeSaved);
			toBeSaved.add(rootOU);
		}

		buildOUTree(rootOU, orgUnitDTOS, dbOuMap, toBeSaved, addressesToBeSaved);

		log.info("orgUnitsTobeSaved = " + toBeSaved.size());
		List<OrgUnit> savedOus = orgUnitService.saveAll(toBeSaved);
		savedOus.forEach(ou -> dbOuMap.put(ou.getOrgId(), ou));

		log.info("OUaddressesToBeSaved = " + addressesToBeSaved.size());
		addressService.saveAll(addressesToBeSaved);

		return dbOuMap;
	}



	private void buildOUTree(OrgUnit parentOU, List<OrganisationAPIController.OrgUnitDTO> orgUnitDTOS, Map<String, OrgUnit> dbOuMap, List<OrgUnit> toBeSaved, List<Address> addressesToBeSaved) {
		for (OrganisationAPIController.OrgUnitDTO dto : orgUnitDTOS) {
			if (Objects.equals(dto.parentId(), parentOU.getOrgId())) {
				OrgUnit matchingOU = dbOuMap.get(dto.id());
				if (matchingOU == null) {
					matchingOU = createOU(dto, parentOU, addressesToBeSaved);
					toBeSaved.add(matchingOU);
				}
				else {
					boolean updated = updateOU(matchingOU, dto, parentOU, addressesToBeSaved);
					if (updated) {
						toBeSaved.add(matchingOU);
					}
				}

				buildOUTree(matchingOU, orgUnitDTOS, dbOuMap, toBeSaved, addressesToBeSaved);
			}
		}
	}

	private OrgUnit createOU(OrganisationAPIController.OrgUnitDTO dto, OrgUnit parent, List<Address> addressesToBeSaved) {
		OrgUnit newOU;
		newOU = new OrgUnit();
		newOU.setOrgId(dto.id());
		newOU.setLongDescription(dto.name());
		newOU.setShortDescription(dto.name());
		newOU.setDefaultCalculationType(CalculationType.CALCULATED);
		newOU.setFourKmRuleAllowed(false);
		if (parent != null) {
			newOU.setParent(parent);
		}

		// Create home address
		Address address = new Address();

		AddressDTO addressDTO = dto.address();
		address.setStreetName(addressDTO.getStreetName());
		address.setStreetNumber(addressDTO.getStreetNumber());
		address.setZipCode(addressDTO.getPostalCode());
		address.setTown(addressDTO.getCity());

//		try {
//			routeService.populateAddressLatLng(address);
//		}
//		catch (JsonProcessingException e) {
//			throw new RuntimeException(e);
//		}

		address.setType(AddressType.WORK);
		address.setOrgUnit(newOU);
		addressesToBeSaved.add(address);

		return newOU;
	}

	private boolean updateOU(OrgUnit ouToUpdate, OrganisationAPIController.OrgUnitDTO dto, OrgUnit parentOU, List<Address> addressesToBeSaved) {
		boolean updated = false;

		// Update parent if needed, case where incoming OU DTO does not have a parent does not reach this code
		// since root is handled before building and updating the rest of the OUs
		if (ouToUpdate.getParent() == null && StringUtils.hasLength(dto.parentId())) {
			updated = true;
			ouToUpdate.setParent(parentOU);
		}
		else if (ouToUpdate.getParent() != null && !Objects.equals(ouToUpdate.getParent().getOrgId(), dto.parentId())) {
			updated = true;
			ouToUpdate.setParent(parentOU);
		}

		if (!Objects.equals(ouToUpdate.getLongDescription(), dto.name())) {
			updated = true;
			ouToUpdate.setLongDescription(dto.name());
		}

		if (!Objects.equals(ouToUpdate.getShortDescription(), dto.name())) {
			updated = true;
			ouToUpdate.setShortDescription(dto.name());
		}

		LocalDateTime now = LocalDateTime.now();
		Address currentAddress = addressService.getOfficialOuAddress(ouToUpdate, now);
		AddressDTO addressDTO = dto.address();
		if (isAddressChanged(currentAddress, addressDTO)) {

			// Address has changed!
			if (currentAddress != null) {
				currentAddress.setEndDate(now);
				addressesToBeSaved.add(currentAddress);
			}

			Address address = new Address();
			address.setStreetName(addressDTO.getStreetName());
			address.setStreetNumber(addressDTO.getStreetNumber());
			address.setZipCode(addressDTO.getPostalCode());
			address.setTown(addressDTO.getCity());
			address.setStartDate(now);

//			try {
//				routeService.populateAddressLatLng(address);
//			}
//			catch (JsonProcessingException e) {
//				throw new RuntimeException(e);
//			}

			address.setType(AddressType.WORK);
			address.setOrgUnit(ouToUpdate);
			addressesToBeSaved.add(address);
		}

		return updated;
	}
}
