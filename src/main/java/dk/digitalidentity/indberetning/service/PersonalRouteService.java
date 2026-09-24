package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.PersonalRouteAddressMappingDao;
import dk.digitalidentity.indberetning.model.dao.PersonalRouteDao;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.PersonalRouteAddressMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalRouteService {

    private final PersonalRouteDao personalRouteDao;
    private final PersonalRouteAddressMappingDao mappingDao;

    public PersonalRoute save(PersonalRoute personalRoute) {
        return personalRouteDao.save(personalRoute);
    }

    public List<PersonalRoute> findAll() {
        return personalRouteDao.findAll();
    }

    public List<PersonalRoute> saveAll(List<PersonalRoute> allPersonalRoutes) {
        return personalRouteDao.saveAll(allPersonalRoutes);
    }

    public void deleteRoute(PersonalRoute personalRoute) {
        personalRouteDao.delete(personalRoute);
    }

    public List<PersonalRoute> findByPerson(Person person) {
        return personalRouteDao.findByPersonId(person);
    }

    public PersonalRoute findById(long id) {
        return personalRouteDao.findById(id).orElse(null);
    }

    // Address mappings
    public PersonalRouteAddressMapping save(PersonalRouteAddressMapping personalRouteAddressMapping) {
        return mappingDao.save(personalRouteAddressMapping);
    }

    public List<PersonalRouteAddressMapping> findByPersonalRoute(PersonalRoute personalRoute) {
        return mappingDao.findByPersonalRoute(personalRoute);
    }
    public List<PersonalRouteAddressMapping> saveAllMappings(List<PersonalRouteAddressMapping> personalRouteAddressMappings) {
        return mappingDao.saveAll(personalRouteAddressMappings);
    }

    public void delete(PersonalRouteAddressMapping personalRouteAddressMapping) {
        mappingDao.delete(personalRouteAddressMapping);
    }
    public void deleteAll(List<PersonalRouteAddressMapping> personalRouteAddressMappings) {
        mappingDao.deleteAll(personalRouteAddressMappings);
    }
}
