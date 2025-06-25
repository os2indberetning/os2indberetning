package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.LicensePlate;
import dk.digitalidentity.indberetning.model.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicensePlateDao extends JpaRepository<LicensePlate, Long> {
    LicensePlate findByRegistrationNumber(String registrationNumber);
    List<LicensePlate> findByPersonId(long id);

    List<LicensePlate> findByPersonAndPrimeTrue(Person person);
}