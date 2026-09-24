package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.dao.AddressDao;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.mockfactory.AddressFactory;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressDTOServiceTest {

    @Mock
    private AddressDao addressDao;
    @Mock
    private OS2indberetningConfiguration configuration;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AddressService addressService;

    private Person testPerson;
    private OrgUnit testOrgUnit;
    private Employment testEmployment;

    @BeforeEach
    void setUp() {
        testPerson = new Person();
        testPerson.setId(1L);
        testPerson.setFirstName("John");
        testPerson.setLastName("Doe");

        testOrgUnit = new OrgUnit();
        testOrgUnit.setId(1L);
        testOrgUnit.setShortDescription("Test OU");

        testEmployment = Employment.builder()
            .id(1L)
            .person(testPerson)
            .orgUnit(testOrgUnit)
            .build();
    }

    @Test
    void getHomeAddressByDate_withDeviation_returnsDeviation() {
        Address deviatee = AddressFactory.home(1L, "Home St", LocalDateTime.now().minusYears(1));
        Address deviation = AddressFactory.deviatingHome(2L, deviatee, LocalDateTime.now().minusMonths(6), null);
        deviatee.setPerson(testPerson);
        testPerson.setAddresses(new LinkedHashSet<>(List.of(deviatee)));

        when(addressDao.findByDeviatingAddressAndPerson(deviatee, testPerson)).thenReturn(List.of(deviation));

        Address result = addressService.getHomeAddressByDate(testPerson, LocalDateTime.now());

        assertThat(result).isEqualTo(deviation);
    }

    @Test
    void getWorkAddressByDate_deviatingTakesPrecedence_overOfficial() {
        LocalDateTime now = LocalDateTime.now();

        Address workAddress1 = AddressFactory.work(1L, "Work St 1", now.minusYears(2));
        Address workAddress2 = AddressFactory.work(2L, "Work St 2", now.minusYears(1));
        testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(workAddress1, workAddress2)));

        Address deviation = AddressFactory.deviatingWork(3L, testEmployment.getPerson(), testEmployment.getOrgUnit(), now.minusMonths(6), null);

        when(addressDao.findByTypeAndPerson(AddressType.DWORK, testPerson)).thenReturn(List.of(deviation));

        Address result = addressService.getWorkAddressByDate(testEmployment, now);

        assertThat(result).isEqualTo(deviation);
    }

    @Test
    void getAvailableAddresses_combinesAllThreeSources() {
        Address homeAddr = AddressFactory.home(1L, "Home St", LocalDateTime.now().minusYears(1));
        homeAddr.setPerson(testPerson);

        Address workAddr = AddressFactory.work(1L, "Work St", LocalDateTime.now().minusYears(1));
        workAddr.setOrgUnit(testOrgUnit);

        Address standardAddr = AddressFactory.standard(1L, "Standard St", LocalDateTime.now());

        when(addressDao.findByPerson(testPerson)).thenReturn(List.of(homeAddr));
        when(addressDao.findAddressByOrgUnit(testOrgUnit)).thenReturn(List.of(workAddr));
        when(addressDao.findByStandardAddressTrue()).thenReturn(List.of(standardAddr));
        testPerson.setEmployments(List.of(testEmployment));

        List<Address> result = addressService.getAvailableAddresses(testPerson);

        assertThat(result).hasSize(3);
    }

    @Nested
    class GetDeviations {

        @Test
        void shouldReturnDeviationWhenActiveAndOpenEnded() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address base = AddressFactory.home(1L, "Home St", now.minusYears(1));
            Address deviation = AddressFactory.deviatingHome(2L, base, now.minusMonths(3), null);

            when(addressDao.findByDeviatingAddressAndPerson(base, testPerson)).thenReturn(List.of(deviation));

            // Act
            Address result = addressService.getDeviations(testPerson, base, now);

            // Assert
            assertThat(result).isEqualTo(deviation);
        }

        @Test
        void shouldReturnBaseAddressWhenDeviationHasExpired() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address base = AddressFactory.home(1L, "Home St", now.minusYears(1));
            // endDate is in the past — deviation is no longer active
            Address expiredDeviation = AddressFactory.deviatingHome(2L, base, now.minusMonths(6), now.minusMonths(1));

            when(addressDao.findByDeviatingAddressAndPerson(base, testPerson)).thenReturn(List.of(expiredDeviation));

            // Act
            Address result = addressService.getDeviations(testPerson, base, now);

            // Assert
            assertThat(result).isEqualTo(base);
        }

        @Test
        void shouldReturnBaseAddressWhenDeviationStartsInTheFuture() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address base = AddressFactory.home(1L, "Home St", now.minusYears(1));
            Address futureDeviation = AddressFactory.deviatingHome(2L, base, now.plusDays(1), null);

            when(addressDao.findByDeviatingAddressAndPerson(base, testPerson)).thenReturn(List.of(futureDeviation));

            // Act
            Address result = addressService.getDeviations(testPerson, base, now);

            // Assert
            assertThat(result).isEqualTo(base);
        }

        @Test
        void shouldReturnMostRecentDeviationWhenMultipleAreActive() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address base = AddressFactory.home(1L, "Home St", now.minusYears(2));
            Address olderDeviation = AddressFactory.deviatingHome(2L, base, now.minusMonths(6), null);
            Address newerDeviation = AddressFactory.deviatingHome(3L, base, now.minusMonths(2), null);

            when(addressDao.findByDeviatingAddressAndPerson(base, testPerson)).thenReturn(List.of(olderDeviation, newerDeviation));

            // Act
            Address result = addressService.getDeviations(testPerson, base, now);

            // Assert
            assertThat(result).isEqualTo(newerDeviation);
        }

        @Test
        void shouldReturnBaseAddressWhenNoDeviationsExist() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address base = AddressFactory.home(1L, "Home St", now.minusYears(1));

            when(addressDao.findByDeviatingAddressAndPerson(base, testPerson)).thenReturn(List.of());

            // Act
            Address result = addressService.getDeviations(testPerson, base, now);

            // Assert
            assertThat(result).isEqualTo(base);
        }
    }

    @Nested
    class GetOfficialHomeAddressDTO {

        @Test
        void shouldReturnSingleAddressWithoutDateFiltering() {
            // Arrange
            Address only = AddressFactory.home(1L, "Only St", LocalDateTime.now().minusYears(1));
            testPerson.setAddresses(new LinkedHashSet<>(List.of(only)));

            // Act
            Address result = addressService.getOfficialHomeAddress(testPerson, LocalDateTime.now());

            // Assert — size==1 shortcut returns whatever is there, no type/date filtering
            assertThat(result).isEqualTo(only);
        }

        @Test
        void shouldReturnLatestAddressStartedBeforeQueryDate() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address older = AddressFactory.home(1L, "Old St", now.minusYears(2));
            Address newer = AddressFactory.home(2L, "New St", now.minusMonths(6));
            testPerson.setAddresses(new LinkedHashSet<>(List.of(older, newer)));

            // Act
            Address result = addressService.getOfficialHomeAddress(testPerson, now);

            // Assert
            assertThat(result).isEqualTo(newer);
        }

        @Test
        void shouldIgnoreAddressWhoseStartDateIsInTheFuture() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address current = AddressFactory.home(1L, "Current St", now.minusMonths(6));
            Address future = AddressFactory.home(2L, "Future St", now.plusDays(1));
            testPerson.setAddresses(new LinkedHashSet<>(List.of(current, future)));

            // Act
            Address result = addressService.getOfficialHomeAddress(testPerson, now);

            // Assert
            assertThat(result).isEqualTo(current);
        }

        @Test
        void shouldReturnNullWhenPersonHasNoAddresses() {
            // Arrange
            testPerson.setAddresses(new LinkedHashSet<>());

            // Act
            Address result = addressService.getOfficialHomeAddress(testPerson, LocalDateTime.now());

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    class GetWorkAddressDTOByDate {

        @Test
        void shouldReturnSingleAddressDirectlyWithoutDateFiltering() {
            // Arrange
            Address only = AddressFactory.work(1L, "Work St", LocalDateTime.now().minusYears(1));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(only)));

            // Act
            Address result = addressService.getWorkAddressByDate(testEmployment, LocalDateTime.now());

            // Assert — size==1 shortcut bypasses date logic
            assertThat(result).isEqualTo(only);
        }

        @Test
        void shouldReturnNullWhenNoOfficialAddressMatchesDate() {
            // Arrange — both addresses start in the future so getOfficialOuAddress returns null
            LocalDateTime now = LocalDateTime.now();
            Address future1 = AddressFactory.work(1L, "Future St 1", now.plusDays(1));
            Address future2 = AddressFactory.work(2L, "Future St 2", now.plusDays(2));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(future1, future2)));

            // Act
            Address result = addressService.getWorkAddressByDate(testEmployment, now);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnOfficialAddressWhenNoDeviationExists() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address older = AddressFactory.work(1L, "Old Work St", now.minusYears(2));
            Address newer = AddressFactory.work(2L, "New Work St", now.minusMonths(3));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(older, newer)));

            when(addressDao.findByTypeAndPerson(AddressType.DWORK, testPerson)).thenReturn(List.of());

            // Act
            Address result = addressService.getWorkAddressByDate(testEmployment, now);

            // Assert
            assertThat(result).isEqualTo(newer);
        }
    }

    @Nested
    class GetOfficialOuAddressDTO {

        @Test
        void shouldReturnSingleAddressWithoutDateFiltering() {
            // Arrange
            Address only = AddressFactory.work(1L, "Only Work St", LocalDateTime.now().minusYears(1));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(only)));

            // Act
            Address result = addressService.getOfficialOuAddress(testOrgUnit, LocalDateTime.now());

            // Assert
            assertThat(result).isEqualTo(only);
        }

        @Test
        void shouldReturnLatestWorkAddressStartedBeforeQueryDate() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address older = AddressFactory.work(1L, "Old Work", now.minusYears(2));
            Address newer = AddressFactory.work(2L, "New Work", now.minusMonths(4));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(older, newer)));

            // Act
            Address result = addressService.getOfficialOuAddress(testOrgUnit, now);

            // Assert
            assertThat(result).isEqualTo(newer);
        }

        @Test
        void shouldIgnoreNonWorkTypeAddresses() {
            // Arrange — DWORK addresses must not be selected as the official address
            LocalDateTime now = LocalDateTime.now();
            Address workAddr = AddressFactory.work(1L, "Work St", now.minusYears(1));
            Address dworkAddr = AddressFactory.deviatingWork(2L, testPerson, testOrgUnit, now.minusMonths(3), null);
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(workAddr, dworkAddr)));

            // Act
            Address result = addressService.getOfficialOuAddress(testOrgUnit, now);

            // Assert
            assertThat(result).isEqualTo(workAddr);
        }

        @Test
        void shouldReturnNullWhenAllAddressesStartInTheFuture() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            Address future1 = AddressFactory.work(1L, "Future 1", now.plusDays(1));
            Address future2 = AddressFactory.work(2L, "Future 2", now.plusDays(2));
            testOrgUnit.setAddresses(new LinkedHashSet<>(List.of(future1, future2)));

            // Act
            Address result = addressService.getOfficialOuAddress(testOrgUnit, now);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    class GetAvailableAddresses {

        @Test
        void shouldExcludeStandardAddressesWhenFlagIsFalse() {
            // Arrange
            testPerson.setEmployments(List.of(testEmployment));
            when(addressDao.findByPerson(testPerson)).thenReturn(List.of());
            when(addressDao.findAddressByOrgUnit(testOrgUnit)).thenReturn(List.of());

            // Act
            List<Address> result = addressService.getAvailableAddresses(testPerson, false);

            // Assert — findByStandardAddressTrue must not have been called, result is empty
            assertThat(result).isEmpty();
        }

        @Test
        void shouldIncludeStandardAddressesWhenFlagIsTrue() {
            // Arrange
            Address standardAddr = AddressFactory.standard(1L, "Standard St", LocalDateTime.now());
            testPerson.setEmployments(List.of(testEmployment));
            when(addressDao.findByPerson(testPerson)).thenReturn(List.of());
            when(addressDao.findAddressByOrgUnit(testOrgUnit)).thenReturn(List.of());
            when(addressDao.findByStandardAddressTrue()).thenReturn(List.of(standardAddr));

            // Act
            List<Address> result = addressService.getAvailableAddresses(testPerson, true);

            // Assert
            assertThat(result).containsExactly(standardAddr);
        }
    }

    @Nested
    class AreAddressesCloseToEachOther {

        @ParameterizedTest(name = "longDiff={2}, latDiff={3} => close={4}")
        @CsvSource({
            "10.0, 56.0, 10.0005, 56.0005, true",   // both diffs well within 0.001
            "10.0, 56.0, 10.0009, 56.0009, true",   // both diffs just inside threshold
            "10.0, 56.0, 10.0020, 56.0000, false",  // longitude clearly outside threshold
            "10.0, 56.0, 10.0000, 56.0020, false",  // latitude clearly outside threshold
            "10.0, 56.0, 10.0020, 56.0020, false",  // both diffs clearly outside
        })
        void shouldReturnExpectedResultForCoordinatePair(
                double longA, double latA, double longB, double latB, boolean expected) {
            assertThat(addressService.areAddressesCloseToEachOther(longA, latA, longB, latB))
                .isEqualTo(expected);
        }
    }
}
