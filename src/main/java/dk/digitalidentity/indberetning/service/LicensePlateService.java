package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.LicensePlateDao;
import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LicensePlateService {
    private final LicensePlateDao licensePlateDao;
    private final SecurityUtil securityUtil;

    public LicensePlate getByRegistrationNumber(String registrationNumber) {
        return licensePlateDao.findByRegistrationNumber(registrationNumber);
    }

    public LicensePlate getById(long id) {
        return licensePlateDao.findById(id).orElse(null);
    }

    public List<LicensePlate> getAll() {
        return licensePlateDao.findAll();
    }

    public List<LicensePlate> getByPersonId(long id) {
        return licensePlateDao.findByPersonId(id);
    }

    public LicensePlate save(LicensePlate licensePlate) {
        return licensePlateDao.save(licensePlate);
    }

    public void saveAll(List<LicensePlate> licensePlates) {
        licensePlateDao.saveAll(licensePlates);
    }

    public void delete(long id) { licensePlateDao.deleteById(id); }

    public void makePlatePrime(LicensePlate newPrimaryPlate) {
        List<LicensePlate> secondaryPlates = getByPersonId(securityUtil.getPerson().getId());
        for (LicensePlate secPlate : secondaryPlates ) { secPlate.setPrime(secPlate == newPrimaryPlate); }
        saveAll(secondaryPlates);
    }

    public LicensePlate create(LicensePlate licensePlate) {
        boolean foundPrimary = false;
        List<LicensePlate> anyPrimaries = getByPersonId(securityUtil.getPerson().getId());
        for (LicensePlate plate : anyPrimaries) {
            if(plate.isPrime()) {
                foundPrimary = true;
                break;
            }
        }
        if(!foundPrimary) { licensePlate.setPrime(true); }
        return licensePlateDao.save(licensePlate);
    }

    // Just return the first result we find
    public LicensePlate getPrimaryPlateByPersonId(Person person) {
        return licensePlateDao.findByPersonAndPrimeTrue(person).stream().findFirst().orElse(null);
    }
}