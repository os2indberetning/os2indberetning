package dk.digitalidentity.indberetning.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.digitalidentity.indberetning.exceptions.AddressLookupRuntimeException;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.dao.PersonDao;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRouteAddressMapping;
import dk.digitalidentity.indberetning.model.entity.QGpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.Roles;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.GpsCoordinateService;
import dk.digitalidentity.indberetning.service.LicensePlateService;
import dk.digitalidentity.indberetning.service.OnetimePaymentsCalculatorService;
import dk.digitalidentity.indberetning.service.PersonalRouteService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.RouteService;
import io.swagger.v3.oas.annotations.Hidden;
import dk.digitalidentity.indberetning.service.cms.CmsMessageBundle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Hidden
@Slf4j
@NoRoleRequired
@RestController
@RequiredArgsConstructor
public class PersonalSettingsRestController {
    private final AddressService addressService;
    private final LicensePlateService licensePlateService;
    private final PersonDao personDao;
    private final SecurityUtil securityUtil;
    private final RouteService routeService;
    private final PersonalRouteService personalRouteService;
    private final ReportService reportService;
    private final GpsCoordinateService gpsCoordinateService;
    private final CmsMessageBundle cmsMessageBundle;

    //plate post mappings
    public record PlateRecord(long id, String registrationNumber, String description, boolean prime) {}

    //Create mapping
    @PostMapping("/rest/plate/create")
    public ResponseEntity<String> createPlate(@RequestBody PlateRecord request) {
        LicensePlate plate = new LicensePlate();
        plate.setRegistrationNumber(request.registrationNumber.trim());
        plate.setDescription(request.description.trim());
        plate.setPrime(request.prime);
        plate.setPerson(securityUtil.getPerson());
        licensePlateService.create(plate);
        return ResponseEntity.ok().build();
    }

    //Update mapping
    @PostMapping("/rest/plate/update")
    public ResponseEntity<String> updatePlate(@RequestBody PlateRecord request) {
        LicensePlate plate = licensePlateService.getById(request.id);
        if(plate == null) { return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build(); }
        if(plate.getPerson() != securityUtil.getPerson()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        plate.setRegistrationNumber(request.registrationNumber.trim());
        plate.setDescription(request.description.trim());
        plate.setPrime(request.prime);
        licensePlateService.save(plate);
        return ResponseEntity.ok().build();
    }

    //Delete mapping
    @PostMapping("/rest/plate/delete/{plateId}")
    public ResponseEntity<String> deletePlate(@PathVariable("plateId") long plateId) {
        LicensePlate plate = licensePlateService.getById(plateId);
        if(plate == null) { return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build(); }
        if(plate.getPerson() != securityUtil.getPerson()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        licensePlateService.delete(plate.getId());
        return ResponseEntity.ok().build();
    }

    //Change primary plate mapping
    @PostMapping("/rest/plate/prime/{plateId}")
    public ResponseEntity<String> primePlate(@PathVariable("plateId") long plateId) {
        LicensePlate plate = licensePlateService.getById(plateId);
        if (plate == null) return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        if(plate.getPerson() != securityUtil.getPerson()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        licensePlateService.makePlatePrime(plate);
        return ResponseEntity.ok().build();
    }
    
    //Toggle the setting for receiving notifications via mail
    //Type options are: admin, personal, approver
    @PostMapping("rest/mail/toggle/{type}")
    public ResponseEntity<String> toggleMailNotifications(@PathVariable("type") String type) {
        Person person = securityUtil.getPerson();
        return switch (type) {
            case "admin" ->
            {
                if(!securityUtil.hasRole(Roles.ROLE_ADMINISTRATOR)) {
                    yield new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
                
                person.setReceiveAdminMail(!person.isReceiveAdminMail());
                personDao.save(person);
                
                yield new ResponseEntity<>(HttpStatus.OK);
            }
            
            case "approver" -> {
                if(!securityUtil.hasRole(Roles.ROLE_APPROVER)) {
                    yield new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
    
                person.setReceiveApproverMail(!person.isReceiveApproverMail());
                personDao.save(person);
    
                yield new ResponseEntity<>(HttpStatus.OK);
            }
            
            case "personal" -> {
                if(!securityUtil.hasRole(Roles.ROLE_USER)) {
                    yield new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
    
                person.setReceivePersonalMail(!person.isReceivePersonalMail());
                personDao.save(person);
    
                yield new ResponseEntity<>(HttpStatus.OK);
            }
            
            default -> new ResponseEntity<>("type must be 'admin, personal or approver'",HttpStatus.BAD_REQUEST);
        };
        
    }

    //address post mappings
    public record AddressRecord (long id, Long parentId, String description, AddressType type, double lat, double lon, String house_number, String road, String town, int postcode, Double deviatingKm, LocalDate startDate) {}

    //create mapping
    @PostMapping("/rest/personalAddress/create")
    public ResponseEntity<String> createPersonalAddress(@RequestBody AddressRecord payloadAddr) {
        Address newAddress = new Address();
        newAddress.setStreetName(payloadAddr.road);
        newAddress.setStreetNumber(payloadAddr.house_number);
        newAddress.setZipCode(payloadAddr.postcode);
        newAddress.setTown(payloadAddr.town);
        newAddress.setLatitude(payloadAddr.lat);
        newAddress.setLongitude(payloadAddr.lon);
        newAddress.setPerson(securityUtil.getPerson());
        newAddress.setType(payloadAddr.type);
        newAddress.setStartDate(LocalDate.now().atStartOfDay());
        if (payloadAddr.description != null) {
            newAddress.setDescription(payloadAddr.description);
        }
        else {
            newAddress.setDescription(newAddress.getType().getDescription());
        }

        if (payloadAddr.parentId != null) {
            Address parentAddress = addressService.getById(payloadAddr.parentId);
            if (parentAddress == null) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }
            if (parentAddress.getType() == AddressType.HOME && parentAddress.getPerson() != securityUtil.getPerson()) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }
            if (parentAddress.getType() == AddressType.WORK) {
                newAddress.setOrgUnit(parentAddress.getOrgUnit());
                if(payloadAddr.deviatingKm != null){
                    newAddress.setHomeToWorkDistanceOverrideDeviation(payloadAddr.deviatingKm);
                }
            }
            newAddress.setDeviatingAddress(parentAddress);


            List<Report> toBeRecalculated = new ArrayList<>();
            if (payloadAddr.startDate != null) {
                LocalDateTime startDate = payloadAddr.startDate.atStartOfDay();
                LocalDateTime currentTime = LocalDateTime.now();
                newAddress.setStartDate(startDate);

                toBeRecalculated = reportService.getByPersonAndDriveDateBetweenAndStatusPending(securityUtil.getPerson(), startDate.toLocalDate(), currentTime.toLocalDate());
            }

            addressService.saveAll(Arrays.asList(newAddress, parentAddress));

            if (!toBeRecalculated.isEmpty()) {
                for (Report report : toBeRecalculated) {
                    report.setRecalculate(true);
                }

                reportService.saveAll(toBeRecalculated);
                return ResponseEntity.ok().body(Integer.toString(toBeRecalculated.size()));
            }
        }
        else {
            addressService.save(newAddress);
        }
        return ResponseEntity.ok().build();
    }

    //delete mapping
    @PostMapping("/rest/personalAddress/delete/{id}")
    public ResponseEntity<String> deletePersonalAddress(@PathVariable("id") long id) {
        Address address = addressService.getById(id);
        if (address == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (address.getPerson() != securityUtil.getPerson()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if(address.getType() == AddressType.ALTERNATIVE) {
            addressService.delete(address.getId());
        }
        else {
            address.setEndDate(LocalDate.now().atStartOfDay());
            addressService.save(address);
        }
        return ResponseEntity.ok().build();
    }

    //edit mapping
    @PostMapping("/rest/personalAddress/edit")
    public ResponseEntity<String> editPersonalAddress(@RequestBody AddressRecord payloadAddr) {
        Address addr = addressService.getById(payloadAddr.id);
        if (addr == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        if (addr.getPerson() != securityUtil.getPerson()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        addr.setStreetName(payloadAddr.road);
        addr.setStreetNumber(payloadAddr.house_number);
        addr.setZipCode(payloadAddr.postcode);
        addr.setTown(payloadAddr.town);
        addr.setLatitude(payloadAddr.lat);
        addr.setLongitude(payloadAddr.lon);
        addr.setType(payloadAddr.type);
        if (payloadAddr.description != null) {
            addr.setDescription(payloadAddr.description);
        }

        List<Report> toBeRecalculated = new ArrayList<>();
        if (payloadAddr.parentId != null) {
            Address paddr = addressService.getById(payloadAddr.parentId);
            if (paddr == null) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }
            if (paddr.getType() == AddressType.HOME && paddr.getPerson() != securityUtil.getPerson()) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }

            if (payloadAddr.startDate != null) {
                boolean startDateChanged = !Objects.equals(payloadAddr.startDate.atStartOfDay(), addr.getStartDate());
                boolean homeToWorkOverrideChanged = false;
                if (payloadAddr.deviatingKm != null && !Objects.equals(payloadAddr.deviatingKm, addr.getHomeToWorkDistanceOverrideDeviation())) {
                     homeToWorkOverrideChanged = Math.abs(payloadAddr.deviatingKm - addr.getHomeToWorkDistanceOverrideDeviation()) > 0.01;
                }
                if (startDateChanged || homeToWorkOverrideChanged) {
                        LocalDateTime startDate = payloadAddr.startDate.atStartOfDay();
                        LocalDateTime currentStartDate = addr.getStartDate();

                        LocalDateTime dateToRecalculateFrom = startDate.isBefore(currentStartDate) ? startDate : currentStartDate;

                        LocalDateTime currentTime = LocalDateTime.now();
                        addr.setStartDate(startDate);

                        toBeRecalculated = reportService.getByPersonAndDriveDateBetweenAndStatusPending(securityUtil.getPerson(), dateToRecalculateFrom.toLocalDate(), currentTime.toLocalDate());
                }
            }

            if (paddr.getType() == AddressType.WORK) {
                addr.setOrgUnit(paddr.getOrgUnit());
                if(payloadAddr.deviatingKm != null){
                    addr.setHomeToWorkDistanceOverrideDeviation(payloadAddr.deviatingKm);
                }
            }

            addr.setDeviatingAddress(paddr);
        }
        addressService.save(addr);

        if (!toBeRecalculated.isEmpty()) {
            for (Report report : toBeRecalculated) {
                report.setRecalculate(true);
            }

            reportService.saveAll(toBeRecalculated);
            return ResponseEntity.ok().body(Integer.toString(toBeRecalculated.size()));
        }

        return ResponseEntity.ok().build();
    }
    record PersonalRouteRecord(long id, String description, String startAddress, String stopAddress, String routeGeometry, List<AddressRecord> addressList) {}
    @PostMapping("/rest/personalRoutes/create")
    public ResponseEntity<?> createPersonalRoute(@RequestBody PersonalRouteRecord personalRouteRecord) throws JsonProcessingException {
        PersonalRoute personalRoute = new PersonalRoute();
        personalRoute.setPersonId(securityUtil.getPerson());
        personalRoute.setDescription(personalRouteRecord.description);
        personalRoute.setRouteGeometry(routeService.routeEncoder(personalRouteRecord.routeGeometry));
        personalRoute.setEnd(personalRouteRecord.stopAddress);
        personalRoute.setStart(personalRouteRecord.startAddress);

        personalRouteService.save(personalRoute);

        ArrayList<PersonalRouteAddressMapping> mappings = new ArrayList<>();
        ArrayList<Address> addresses = new ArrayList<>();
        for (int i = 0; i < personalRouteRecord.addressList.size(); i++) {
            AddressRecord addressRecord = personalRouteRecord.addressList.get(i);
            Address address = new Address();
            address.setStreetName(addressRecord.road);
            address.setStreetNumber(addressRecord.house_number);
            address.setZipCode(addressRecord.postcode);
            address.setTown(addressRecord.town);
            try {
                address = addressService.populateAddressLatLng(address, new HashMap<>());
            } catch (JsonProcessingException ex) {
                return ResponseEntity.badRequest().build();
            } catch (AddressLookupRuntimeException ex) {
                return ResponseEntity.badRequest().body(ex.getMessage());
            }

            address.setPerson(securityUtil.getPerson());
            address.setType(AddressType.PERSONAL_ROUTE_POINT);
            PersonalRouteAddressMapping personalRouteAddressMapping = new PersonalRouteAddressMapping(i, personalRoute, address);

            addresses.add(address);
            mappings.add(personalRouteAddressMapping);
        }

        mappings.getFirst().setStartPoint(true);
        mappings.getFirst().setWaypoint(false);

        mappings.getLast().setEndPoint(true);
        mappings.getLast().setWaypoint(false);

        addressService.saveAll(addresses);
        personalRouteService.saveAllMappings(mappings);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/personalRoutes/delete")
    public ResponseEntity<String> deletePersonalRoute(@RequestParam("id") long id) {
        PersonalRoute personalRoute = personalRouteService.findById(id);
        if (personalRoute != null) {
            personalRouteService.deleteAll(personalRoute.getPersonalRouteAddressMappings());
            personalRouteService.deleteRoute(personalRoute);
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/rest/personalRoutes/edit")
    public ResponseEntity<String> editPersonalRoute(@RequestBody PersonalRouteRecord personalRouteRecord) throws JsonProcessingException {
        PersonalRoute personalRoute = personalRouteService.findById(personalRouteRecord.id);

        if (personalRoute == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        else {
            // These are okay
            personalRoute.setDescription(personalRouteRecord.description);
            personalRoute.setStart(personalRouteRecord.startAddress);
            personalRoute.setEnd(personalRouteRecord.stopAddress);
            personalRoute.setRouteGeometry(routeService.routeEncoder(personalRouteRecord.routeGeometry));

            // Clear the old values
            personalRouteService.deleteAll(personalRoute.getPersonalRouteAddressMappings());

            // Map new ones
            ArrayList<PersonalRouteAddressMapping> mappings = new ArrayList<>();
            ArrayList<Address> addresses = new ArrayList<>();
            for (int i = 0; i < personalRouteRecord.addressList.size(); i++) {
                AddressRecord addressRecord = personalRouteRecord.addressList.get(i);

                Address address = new Address();
                address.setStreetName(addressRecord.road);
                address.setStreetNumber(addressRecord.house_number);
                address.setZipCode(addressRecord.postcode);
                address.setTown(addressRecord.town);
                address = addressService.populateAddressLatLng(address, new HashMap<>());
                address.setPerson(securityUtil.getPerson());
                address.setType(AddressType.PERSONAL_ROUTE_POINT);
                PersonalRouteAddressMapping personalRouteAddressMapping = new PersonalRouteAddressMapping(i, personalRoute, address);

                addresses.add(address);
                mappings.add(personalRouteAddressMapping);
            }

            mappings.getFirst().setStartPoint(true);
            mappings.getFirst().setWaypoint(false);

            mappings.getLast().setEndPoint(true);
            mappings.getLast().setWaypoint(false);

            addressService.saveAll(addresses);
            personalRoute.setPersonalRouteAddressMappings(mappings);
            personalRouteService.saveAllMappings(mappings);
            personalRouteService.save(personalRoute);



            return ResponseEntity.ok().build();
        }
    }

    record ResponseAddressList(List<String> addresses) {}
    @GetMapping("/rest/personalRoute/getAddresses")
    public ResponseEntity<?> getPersonalAddresses(@RequestParam long id) {
        PersonalRoute personalRoute = personalRouteService.findById(id);
        if (personalRoute == null) {
            log.warn("The route was not found on id: {}", id);
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(new ResponseAddressList(personalRoute.getAddressesAsStrings()), HttpStatus.OK);
    }

    // Might be worth to move this code somewhere else
    @PostMapping("/rest/notification/available")
    public ResponseEntity<?> getNotification() {
        String notificationText = cmsMessageBundle.getText("cms.notification.body");
        if (notificationText == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(notificationText);
    }
}
