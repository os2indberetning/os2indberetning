package dk.digitalidentity.indberetning.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.indberetning.security.SecurityUtil;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.exceptions.AddressLookupRuntimeException;
import dk.digitalidentity.indberetning.model.dao.AddressDao;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
@RequiredArgsConstructor
public class AddressService {

	@Autowired
	@Qualifier("defaultRestClient")
	private RestClient restClient;

    private final AddressDao addressDao;
	private final OS2indberetningConfiguration configuration;
    @Autowired
    private SecurityUtil securityUtil;


	public void delete(long id) {
		addressDao.deleteById(id);
	}

	public List<Address> getAll() {
		return addressDao.findAll();
	}

	public Address getById(long id) {
		return addressDao.findById(id).orElse(null);
	}

	public List<Address> getByPerson(Person person) {
		return addressDao.findByPerson(person);
	}

	public List<Address> getDirtyAddresses() {
		return addressDao.findByIsDirtyTrue();
	}

	public List<Address> getByDirtyStringAndType(String dirtyString, AddressType addressType) {
		return addressDao.findByDirtyStringAndType(dirtyString, addressType);
	}

	public void processUnprocessedAddresses() {
        List<Address> toBeProcessed = addressDao.findByIsDirtyFalseAndLatitudeAndLongitudeAndCoordinateFetchTriesLessThan(0, 0, 5);

        int totalElements = toBeProcessed.size();
        int count = 0;

        Map<String, Address> lookupTable = new HashMap<>();

        ArrayList<Address> toBeSaved = new ArrayList<>();
        for (Address address : toBeProcessed) {
			try {
                address.setCoordinateFetchTries(address.getCoordinateFetchTries() + 1);
				populateAddressLatLng(address, lookupTable);

                toBeSaved.add(address);

                if (count % 100 == 0 && count != 0) {
                    log.info("Processed " + count + " addresses out of " + totalElements);
                }

                // Save the list every 500 elements
                if (count % 500 == 0 && count != 0) {
                    log.info("Saving!");
                    saveAll(toBeSaved);
                    toBeSaved.clear();
                }

                count++;
			}
			catch (Exception ignored) {
                // We try multiple times to populate lat/lng if it does not work we add it to Address wash.
			}
		}
        saveAll(toBeSaved);

        List<Address> couldNotFetch = addressDao.findByCoordinateFetchTriesGreaterThanEqual(5);
        couldNotFetch.forEach(address -> address.setDirty(true));
        saveAll(couldNotFetch);
    }

	//returns primary or creates a fake primary around Gunnar Clausens Vej 68, so the maps still works.
	public Address getPrimary() {
		return addressDao.findByPrimaryTrue().orElseGet(() -> {
			Address address = new Address();
			address.setLatitude(56.1066);
			address.setLongitude(10.14476);
			return address;
		});
	}

	// Work addresses
	public record WorkAddressesResult(Employment employment, Address actualAddress, Address officialAddress) {}
	public Set<WorkAddressesResult> getWorkByDate(Person person, LocalDateTime dateTime) {
		Set<WorkAddressesResult> workAddresses = new HashSet<>();
		person.getEmployments().forEach(employment -> {
			if (!workAddresses.stream().anyMatch(workAddressesResult -> workAddressesResult.employment.getOrgUnit().equals(employment.getOrgUnit()))) {
				workAddresses.add(new WorkAddressesResult(employment, getDeviatingOuAddress(employment.getOrgUnit().getAddresses().stream().filter(address -> address.getType().equals(AddressType.DWORK)).collect(Collectors.toList()), employment, dateTime), getOfficialOuAddress(employment.getOrgUnit(), dateTime)));
			}
		});
		return workAddresses;
	}

	public Address getWorkAddressByDate(Employment employment, LocalDateTime dateTime) {
		Set<Address> addresses = employment.getOrgUnit().getAddresses();

		// Get the address with the latest startDate that is before the selected time.
		// All addresses below has been 'overridden' by the max one,
		// and any address starting later cannot be used at the selected time
		if (addresses.size() == 1) {
			return addresses.stream().findAny().orElse(null);
		}

		Address officialOuAddress = getOfficialOuAddress(employment.getOrgUnit(), dateTime);

		if (officialOuAddress != null) {
			Address deviatingOuAddress = getDeviatingOuAddress(addressDao.findByTypeAndPerson(AddressType.DWORK, employment.getPerson()), employment, dateTime);

			return deviatingOuAddress != null ? deviatingOuAddress : officialOuAddress;
		}
		return null;
	}

	public Address getOfficialOuAddress(OrgUnit orgUnit, LocalDateTime localDateTime) {
		Set<Address> orgUnitAddresses = orgUnit.getAddresses();

		// Get the address with the latest startDate that is before the selected time.
		// All addresses below has been 'overridden' by the max one,
		// and any address starting later cannot be used at the selected time
		if (orgUnitAddresses.size() == 1) {
			return orgUnitAddresses.stream().findAny().orElse(null);
		}

		Optional<Address> activeAddress = orgUnitAddresses.stream()
				.filter(address -> AddressType.WORK.equals(address.getType()))
				.filter(address -> address.getStartDate() == null || address.getStartDate().isBefore(localDateTime))
				.max(Comparator.comparing(Address::getStartDate,
						Comparator.nullsFirst(Comparator.naturalOrder())
				));
		return activeAddress.orElse(null);
	}

	public Address getDeviatingOuAddress(List<Address> deviations, Employment employment, LocalDateTime dateTime) {
		Optional<Address> deviation = deviations.stream()
				.filter(address -> address.getOrgUnit() == employment.getOrgUnit())
				.filter(address -> address.getPerson() == employment.getPerson())
				.filter(address -> address.getStartDate() == null || address.getStartDate().isEqual(dateTime) || address.getStartDate().isBefore(dateTime))
				.filter(address -> address.getEndDate() == null || address.getEndDate().isAfter(dateTime))
				.max(Comparator.comparing(Address::getStartDate,
						Comparator.nullsFirst(Comparator.naturalOrder())
				));

		return deviation.orElse(null);
	}



	// Home addresses
	@CacheEvict(value = "getHomeAddressByDate", allEntries=true)
	public void clearGetHomeAddressByDateCache() {
	}

	@Cacheable("getHomeAddressByDate")
    public Address getHomeAddressByDateCached(Person person, LocalDateTime dateTime) {
		return getHomeAddressByDate(person, dateTime);
	}

	// Returns all addresses including deviating addresses
	public Address getHomeAddressByDate(Person person, LocalDateTime dateTime) {
		Address officialHomeAddress = getOfficialHomeAddress(person, dateTime);

		if (officialHomeAddress != null) {
			return getDeviations(person, officialHomeAddress, dateTime);
		}

		return null;
	}

	// This returns the current address without taking deviations into account
	public Address getOfficialHomeAddress(Person person, LocalDateTime dateTime) {
		Set<Address> personAddresses = person.getAddresses();

		// Get the address with the latest startDate that is before the selected time.
		// All addresses below has been 'overridden' by the max one,
		// and any address starting later cannot be used at the selected time
		if (personAddresses.size() == 1) {
			return personAddresses.stream().findAny().orElse(null);
		}

		Optional<Address> addresses = personAddresses.stream()
                .filter(address -> AddressType.HOME.equals(address.getType()))
                .filter(address -> address.getStartDate() == null || address.getStartDate().isBefore(dateTime))
				.max(Comparator.comparing(Address::getStartDate,
						Comparator.nullsFirst(Comparator.naturalOrder())
				));

		return addresses.orElse(null);
	}

	public List<Address> getStandardAddresses() {
		return addressDao.findByStandardAddressTrue();
	}

	public void makeAddressPrimary(Address newPrimaryAddress) {
		List<Address> addresses = getAll();
		for (Address address : addresses) {
			address.setPrimary(address == newPrimaryAddress);
		}
		saveAll(addresses);
	}

	public Address save(Address address) {
		return addressDao.save(address);
	}

	public List<Address> saveAll(List<Address> addresses) {
		return addressDao.saveAllAndFlush(addresses);
	}

	public List<Address> getAvailableAddresses(Person person, boolean includeStandard) {
		List<Address> availableAddresses = new ArrayList<>();
		availableAddresses.addAll(addressDao.findByPerson(person));
		person.getEmployments().forEach(employment -> availableAddresses.addAll(addressDao.findAddressByOrgUnit(employment.getOrgUnit())));
		if (includeStandard)
			availableAddresses.addAll(addressDao.findByStandardAddressTrue());
		return availableAddresses;
	}

	public List<Address> findByPersonalRoute(PersonalRoute personalRoute) {
		return addressDao.findByPersonalRoute(personalRoute);
	}

	//Overloading of above with default parameter true which is compatible with former uses before extended with includeStandard
	public List<Address> getAvailableAddresses(Person person) {
		return getAvailableAddresses(person, true);
	}

	public ArrayList<Address> getAvailableAddressesByDate(Person person, LocalDateTime dateTime, boolean includeStandard) {
		Set<Address> availableAddresses = new HashSet<>();
		
		Address homeAddress = getHomeAddressByDate(person, dateTime);
		if (homeAddress != null) {
			availableAddresses.add(homeAddress);
		}

		person.getEmployments().forEach(employment -> {
			Address workAddress = getWorkAddressByDate(employment, dateTime);
			if (workAddress != null) {
				availableAddresses.add(workAddress);
			}
		});

		availableAddresses.addAll(addressDao.findByPersonAndType(person, AddressType.ALTERNATIVE));
		if (includeStandard) {
			availableAddresses.addAll(addressDao.findByStandardAddressTrue());
		}

		ArrayList<Address> addresses = new ArrayList<>(availableAddresses);
		addresses.sort(Comparator.comparing(Address::getStreetName));

		return addresses;
	}

	public ArrayList<Address> getAvailableAddressesByDate(Person person, LocalDateTime dateTime) {
		return getAvailableAddressesByDate(person, dateTime, true);
	}

    public Address getDeviations(Person person, Address deviatee, LocalDateTime dateTime) {
		List<Address> deviations = addressDao.findByDeviatingAddressAndPerson(deviatee, person);

		Optional<Address> deviation = deviations.stream()
                .filter(address -> address.getStartDate() == null || address.getStartDate().isEqual(dateTime) || address.getStartDate().isBefore(dateTime))
                .filter(address -> address.getEndDate() == null || address.getEndDate().isAfter(dateTime))
                .max(Comparator.comparing(Address::getStartDate,Comparator.nullsFirst(Comparator.naturalOrder())
        ));
        return deviation.orElse(deviatee);
    }

	public boolean areAddressesCloseToEachOther(double longA, double latA, double longB, double latB) {
		double coordinateThreshold = 0.001;
		double longDiff = Math.abs(longA - longB);
		double latDiff = Math.abs(latA - latB);

		return longDiff < coordinateThreshold && latDiff < coordinateThreshold;
	}


	//backend operations
	public Address populateAddressLatLng(Address address, Map<String, Address> lookupTable) throws JsonProcessingException, AddressLookupRuntimeException {
		if(address == null) {
			return null;
		}
		if(address.getLatitude() != 0 && address.getLongitude() != 0) {
			return address;
		}

		String toBePopulatedAddressString = address.getAddressString();
		if (lookupTable.containsKey(toBePopulatedAddressString)) {
			Address cachedAddress = lookupTable.get(toBePopulatedAddressString);
			address.setDirty(cachedAddress.isDirty());
			address.setLatitude(cachedAddress.getLatitude());
			address.setLongitude(cachedAddress.getLongitude());
			return address;
		}

		String addressString = addressToLatLng(address.getStreetName(), address.getStreetNumber(), String.valueOf(address.getZipCode()), true);
		if (!StringUtils.hasLength(addressString)) {
			log.warn("AddressToLatLng returned nothing: " + toBePopulatedAddressString);
			address.setDirty(true);
			return address;
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode addressJson = mapper.readTree(addressString);
		log.trace("");
		log.trace("addressJson = " + addressJson);

		if (addressJson == null || addressJson.isEmpty()) {
			log.warn("No addresses found from supplied address: " + toBePopulatedAddressString);
			address.setDirty(true);
			return address;
		}

		JsonNode accessAddress = addressJson.get(0);
		if (accessAddress == null) {
			log.warn("No addresses found from supplied address: " + toBePopulatedAddressString);
			address.setDirty(true);
			return address;
		}

		if (accessAddress.get("dirty") != null) {
			address.setDirty(true);
		}

		JsonNode accessPoint = accessAddress.get("adgangspunkt");
		if (accessPoint == null) {
			log.error("Address missing accessPoint information");
			return address;
		}

		JsonNode coordinates = accessPoint.get("koordinater");
		if (coordinates == null || coordinates.size() != 2) {
			log.error("Address missing coordinate information");
			return address;
		}

		address.setLatitude(Double.parseDouble(coordinates.get(1).toString()));
		address.setLongitude(Double.parseDouble(coordinates.get(0).toString()));

		return address;
	}

	@Nullable
	public String addressToLatLng(String road, String houseNumber, String postcode) {
		return addressToLatLng(road, houseNumber, postcode, false);
	}

	public String addressToLatLng(String road, String houseNumber, String postcode, boolean retry) {
		JsonNode req = null ;
		try {
			if (houseNumber.contains(",")) {
				String[] split = houseNumber.split(",", 2);
				String potentialHouseNumber = split[0].trim();
				houseNumber = StringUtils.hasLength(potentialHouseNumber) ? potentialHouseNumber : houseNumber;
			}

			req = restClient.get()
					.uri(configuration.getMap().getMapServiceBaseUrl() + "adgangsadresser?vejnavn=" + road + "&husnr=" + houseNumber + "&postnr=" + postcode)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig", response.getStatusCode(), response.getHeaders());
					})
					.body(JsonNode.class);

			if (retry) {
				if (req == null || req.isEmpty() || req.get(0) == null) {
					req = getBestWashedResult(road, houseNumber, postcode);
					return req != null ? req.toString() : null;
				}
			}
		}
		catch (AddressLookupRuntimeException ex) {
			// If address lookup fails with a 4xx error we try to wash the address automatically and try to find coords based on the washed address.
			// If the washed address does not turn op any good data we mark the address as dirty so it can be washed manually.
			req = getBestWashedResult(road, houseNumber, postcode);
		}

		return req != null ? req.toString() : null;
	}

	@Nullable
	private JsonNode getBestWashedResult(String road, String houseNumber, String postcode) {
		JsonNode washedReq = restClient.get()
				.uri(configuration.getMap().getMapServiceBaseUrl() + "datavask/adgangsadresser?betegnelse=" + road + " " + houseNumber + ", " + postcode)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig", res.getStatusCode(), res.getHeaders());
				})
				.body(JsonNode.class);

		if (washedReq == null || washedReq.isEmpty()) {
			log.warn("Could not wash address: road=" + (StringUtils.hasLength(road) ? road : "<null>") + "  houseNumber=" + (StringUtils.hasLength(houseNumber) ? houseNumber : "<null>") + "  postCode=" + (StringUtils.hasLength(postcode) ? postcode : "<null>"));
			return null;
		}

		JsonNode category = washedReq.get("kategori");
		boolean dirty = false;
		if (category == null || category.toString().charAt(1) == 'C') {
			dirty = true;
		}

		JsonNode results = washedReq.get("resultater");
		if (results == null || results.isEmpty()) {
			log.warn("Could not find address from: road=" + (StringUtils.hasLength(road) ? road : "<null>") + "  houseNumber=" + (StringUtils.hasLength(houseNumber) ? houseNumber : "<null>") + "  postCode=" + (StringUtils.hasLength(postcode) ? postcode : "<null>"));
			return null;
		}
		JsonNode bestResult = results.get(0);
		if (bestResult == null || bestResult.get("aktueladresse") == null) {
			log.warn("resultater malformed for: road=" + (StringUtils.hasLength(road) ? road : "<null>") + "  houseNumber=" + (StringUtils.hasLength(houseNumber) ? houseNumber : "<null>") + "  postCode=" + (StringUtils.hasLength(postcode) ? postcode : "<null>"));
			return null;
		}

		JsonNode aktueladresse = bestResult.get("aktueladresse");
		JsonNode href = aktueladresse.get("href");
		if (href == null || href.toString().length() < 3) {
			log.warn("aktueladresse malformed for: road=" + (StringUtils.hasLength(road) ? road : "<null>") + "  houseNumber=" + (StringUtils.hasLength(houseNumber) ? houseNumber : "<null>") + "  postCode=" + (StringUtils.hasLength(postcode) ? postcode : "<null>"));
			return null;
		}

		JsonNode response = restClient.get()
				.uri(configuration.getMap().getMapServiceBaseUrl() + "adgangsadresser?vejnavn=" + aktueladresse.get("vejnavn").toString().substring(1, aktueladresse.get("vejnavn").toString().length() - 1) + "&husnr=" + aktueladresse.get("husnr").toString().substring(1, aktueladresse.get("husnr").toString().length() - 1) + "&postnr=" + aktueladresse.get("postnr").toString().substring(1, aktueladresse.get("postnr").toString().length() - 1))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					throw new AddressLookupRuntimeException("Dataforsyningens kortservice er utilgængelig", res.getStatusCode(), res.getHeaders());
				})
				.body(JsonNode.class);

		if (response == null) {
			log.warn("washed address returned wrong result for: road=" + (StringUtils.hasLength(road) ? road : "<null>") + "  houseNumber=" + (StringUtils.hasLength(houseNumber) ? houseNumber : "<null>") + "  postCode=" + (StringUtils.hasLength(postcode) ? postcode : "<null>"));
			return null;
		}

		if (dirty) {
			((ObjectNode) response.get(0)).put("dirty", true);
		}

		return response;
	}


	public List<Address> filterAddressesByInput(List<Address> addresses, String input) {
		List<Address> result = new ArrayList<>();
		Person person = securityUtil.getPerson();
		if (person == null) {
			return result;
		}

		input = input.toLowerCase();
		if ("hjemme".startsWith(input)) {
			return Collections.singletonList(getHomeAddressByDate(person, LocalDateTime.now()));
		}
		if ("arbejde".startsWith(input)) {
			List<Address> workAddresses = new ArrayList<>();
			for (Employment employment : person.getEmployments()) {
				workAddresses.addAll(employment.getOrgUnit().getAddresses());
			}
			return workAddresses;
		}
		for (Address address : addresses) {
			if (address.getAddressString().toLowerCase().startsWith(input)) {
				result.add(address);
			}
		}
		List<Address> matchingOuAddresses = addressDao.findAddressByOrgUnitNameLike(input, person.getId());
		result.addAll(matchingOuAddresses);

		return result;
	}
}