package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.PersonalRoute;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AddressDao extends JpaRepository<Address, Long> {
    List<Address> findByDirtyStringAndType(String dirtyString, AddressType type);
	List<Address> findByPersonAndType(Person person, AddressType type);
    Optional<Address> findById(long id);
    List<Address> findByPerson(Person person);
    List<Address> findByStandardAddressTrue();
    List<Address> findAddressByOrgUnit(OrgUnit orgUnit);
    Optional<Address> findByPrimaryTrue();

    List<Address> findByPersonalRoute(PersonalRoute personalRoute);
    List<Address> findByIsDirtyTrue();
    List<Address> findByTypeAndPerson(AddressType type, Person person);

    Optional<Address> findByDeviatingAddress_DeviatingAddressAndDeviatingAddress_StartDateAndEndDate(Address deviatingAddress, LocalDateTime startDate, LocalDateTime endDate);

    List<Address> findByDeviatingAddress(Address deviatingAddress);

    List<Address> findByCreatedTimestampGreaterThan(LocalDateTime createdTimestamp);

    List<Address> findByIsDirtyFalseAndLatitudeAndLongitudeAndCoordinateFetchTriesLessThan(double latitude, double longitude, int coordinateFetchTries);

    List<Address> findByCoordinateFetchTriesGreaterThanEqual(int coordinateFetchTries);

    List<Address> findByDeviatingAddressAndPerson(Address deviatingAddress, Person person);

    @Query(nativeQuery = true, value = """

            SELECT a.*
                FROM addresses a
                         LEFT JOIN orgunits o ON a.orgunit_id=o.id
                WHERE
                    (
                        (
                            type="WORK" AND
                            orgunit_id IS NOT NULL AND
                            LOWER(o.long_description) LIKE CONCAT('%', ?1, '%')
                        ) OR
                        (
                            type="STANDARD" AND
                            LOWER(a.description) LIKE CONCAT('%', ?1, '%')
                        ) OR
                        (
                            type="ALTERNATIVE" AND
                            LOWER(a.description) LIKE CONCAT('%', ?1, '%') AND
                            a.person_id=?2
                        )
                    ) AND
                    (
                        (a.person_id IS NULL) OR
                        (a.person_id=?2)
                    )
                LIMIT 10""")
    List<Address> findAddressByOrgUnitNameLike(@Param("name") String input, @Param("personId") long personId);

    List<Address> findByEndDateBefore(LocalDateTime date);

}