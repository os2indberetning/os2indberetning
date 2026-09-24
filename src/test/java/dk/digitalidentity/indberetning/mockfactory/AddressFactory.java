package dk.digitalidentity.indberetning.mockfactory;

import java.time.LocalDateTime;

import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;

public class AddressFactory {
	public static Address basic(Long id, String street, LocalDateTime startDate) {
		Address a = new Address();
		a.setId(id);
		a.setStreetName(street);
		a.setStreetNumber("1");
		a.setZipCode(8000);
		a.setTown("Aarhus");
		a.setLatitude(10.0);
		a.setLongitude(56.0);
		a.setStartDate(startDate);
		return a;
	}

	public static Address home(Long id, String street, LocalDateTime startDate) {
		Address a = basic(id, street, startDate);
		a.setType(AddressType.HOME);
		return a;
	}
	
	public static Address work(Long id, String street, LocalDateTime startDate) {
		Address a = basic(id, street, startDate);
		a.setType(AddressType.WORK);
		return a;
	}
	
	public static Address standard(Long id, String street, LocalDateTime startDate) {
		Address a = basic(id, street, startDate);
		a.setType(AddressType.STANDARD);
		a.setStandardAddress(true);
		return a;
	}
	
	public static Address deviatingHome(Long id, Address deviatee, LocalDateTime start, LocalDateTime end) {
		Address a = home(id, deviatee.getStreetName() + " Dev", start);
		a.setType(AddressType.DHOME);
		a.setDeviatingAddress(deviatee);
		a.setEndDate(end);
		return a;
	}
	
	public static Address deviatingWork(Long id, Person person, OrgUnit orgUnit, LocalDateTime start, LocalDateTime end) {
		Address a = work(id, "Dev Work", start);
		a.setPerson(person);
		a.setOrgUnit(orgUnit);
		a.setType(AddressType.DWORK);
		a.setEndDate(end);
		return a;
	}
}
