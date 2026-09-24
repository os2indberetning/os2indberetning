package dk.digitalidentity.indberetning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.mockfactory.ReportBuilder;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static dk.digitalidentity.indberetning.mockfactory.ReportBuilder.endCoord;
import static dk.digitalidentity.indberetning.mockfactory.ReportBuilder.startCoord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnetimePaymentsCalculatorServiceTest {

    @Mock private AddressService addressService;
    @Mock private OS2indberetningConfiguration configuration;
    @Mock private ReportService reportService;
    @Mock private RouteService routeService;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private OnetimePaymentsCalculatorService service;

    private Person person;
    private Employment employment;
    private Address homeAddress;
    private Address workAddress;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        person = new Person();

        homeAddress = new Address();
        homeAddress.setLatitude(56.0);
        homeAddress.setLongitude(10.0);
        homeAddress.setType(AddressType.HOME);

        workAddress = new Address();
        workAddress.setLatitude(56.1);
        workAddress.setLongitude(10.1);
        workAddress.setType(AddressType.WORK);

        employment = Employment.builder()
                .id(1L)
                .person(person)
                .build();

        lenient().when(addressService.getHomeAddressByDate(eq(person), any())).thenReturn(homeAddress);
        lenient().when(addressService.getWorkAddressByDate(eq(employment), any())).thenReturn(workAddress);
        lenient().when(routeService.distanceBetweenAddresses(homeAddress, workAddress)).thenReturn(5.0);
    }

    // sortByRoute mutates the list it receives, so we must always pass a mutable list to calculate()
    private List<Report> calc(Report... reports) {
        return service.calculate(new ArrayList<>(List.of(reports)));
    }

    // -------------------------------------------------------------------------
    // validateReportInput
    // -------------------------------------------------------------------------
    @Nested
    class ValidateReportInput {

        @Test
        void shouldThrowWhenReportListIsNull() {
            assertThatThrownBy(() -> service.calculate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldThrowWhenReportListIsEmpty() {
            assertThatThrownBy(() -> service.calculate(new ArrayList<>()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldThrowWhenReportsHaveDifferentDriveDates() {
            // Arrange
            Report r1 = new ReportBuilder().person(person).employment(employment).driveDate(LocalDate.of(2024, 11, 1)).build();
            Report r2 = new ReportBuilder().person(person).employment(employment).driveDate(LocalDate.of(2024, 11, 2)).build();

            // Act & Assert
            assertThatThrownBy(() -> calc(r1, r2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DriveDates");
        }

        @Test
        void shouldThrowWhenReportsAreFromDifferentPersons() {
            // Arrange
            Person other = new Person();
            Report r1 = new ReportBuilder().person(person).employment(employment).build();
            Report r2 = new ReportBuilder().person(other).employment(employment).build();

            // Act & Assert
            assertThatThrownBy(() -> calc(r1, r2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same person");
        }

        @Test
        void shouldAcceptSingleReport() {
            // Arrange
            Report r = new ReportBuilder().person(person).employment(employment).build();

            // Act & Assert — no exception
            assertThat(calc(r)).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // calculateDistanceBetweenHomeAndWork
    // -------------------------------------------------------------------------
    @Nested
    class CalculateDistanceBetweenHomeAndWork {

        @Test
        void shouldReturnOverrideDirectlyWhenSet() {
            // Arrange
            employment.setHomeToWorkDistanceOverride(7.5);

            // Act
            double result = service.calculateDistanceBetweenHomeAndWork(employment, LocalDate.of(2024, 11, 15));

            // Assert — override short-circuits all address lookups
            assertThat(result).isEqualTo(7.5);
        }

        @Test
        void shouldReturnZeroWhenWorkAddressIsNull() {
            // Arrange
            when(addressService.getWorkAddressByDate(eq(employment), any())).thenReturn(null);

            // Act
            double result = service.calculateDistanceBetweenHomeAndWork(employment, LocalDate.of(2024, 11, 15));

            // Assert
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        void shouldReturnZeroWhenHomeAddressIsNull() {
            // Arrange
            when(addressService.getHomeAddressByDate(eq(person), any())).thenReturn(null);

            // Act
            double result = service.calculateDistanceBetweenHomeAndWork(employment, LocalDate.of(2024, 11, 15));

            // Assert
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        void shouldReturnDeviationOverrideWhenWorkAddressIsDworkWithOverride() {
            // Arrange
            Address base = new Address();
            workAddress.setType(AddressType.DWORK);
            workAddress.setDeviatingAddress(base);
            workAddress.setHomeToWorkDistanceOverrideDeviation(12.0);

            // Act
            double result = service.calculateDistanceBetweenHomeAndWork(employment, LocalDate.of(2024, 11, 15));

            // Assert — deviation override takes priority over route calculation
            assertThat(result).isEqualTo(12.0);
        }

        @Test
        void shouldCallRouteServiceWhenNoOverridesAreSet() throws JsonProcessingException {
            // Arrange
            when(routeService.distanceBetweenAddresses(homeAddress, workAddress)).thenReturn(8.3);

            // Act
            double result = service.calculateDistanceBetweenHomeAndWork(employment, LocalDate.of(2024, 11, 15));

            // Assert
            assertThat(result).isEqualTo(8.3);
        }
    }

    // -------------------------------------------------------------------------
    // applyFourKmRule — tested via calculate()
    // -------------------------------------------------------------------------
    @Nested
    class ApplyFourKmRule {

        @Test
        void shouldDeductUpTo4kmFromFirstReport() {
            // Arrange — 10km route, fourKmRule, no home start/end so no homeToWork subtraction
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).fourKmRule(true)
                    .calculationType(CalculationType.READ)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert — 10 - 4 = 6
            assertThat(result.get(0).getDistance()).isEqualTo(6.0);
        }

        @Test
        void shouldNotDeductMoreThan4kmAcrossMultipleReports() {
            // Arrange — two fourKmRule reports; combined deduction caps at 4
            Report r1 = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(3.0).fourKmRule(true)
                    .calculationType(CalculationType.READ)
                    .build();
            Report r2 = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).fourKmRule(true)
                    .calculationType(CalculationType.READ)
                    .build();

            // Act
            List<Report> result = calc(r1, r2);

            // Assert — r1 consumed 3km of the budget; r2 gets only 1km deducted → 0 + 9 = 9
            double total = result.stream().mapToDouble(Report::getDistance).sum();
            assertThat(total).isCloseTo(9.0, within(0.01));
        }

        @Test
        void shouldZeroOutReportWhenDistanceSmallerThanRemainingBudget() {
            // Arrange — 2km route, full 4km budget available → all consumed, result is 0
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(2.0).fourKmRule(true)
                    .calculationType(CalculationType.READ)
                    .build();

            // Act
            List<Report> result = calc(r);

            assertThat(result.get(0).getDistance()).isEqualTo(0.0);
        }

        @Test
        void shouldNotApplyFourKmRuleToReportsWithFlagOff() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).fourKmRule(false)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert — no deduction
            assertThat(result.get(0).getDistance()).isEqualTo(10.0);
        }
    }

    // -------------------------------------------------------------------------
    // calculateAmountToReimburse — tested via calculate()
    // -------------------------------------------------------------------------
    @Nested
    class CalculateAmountToReimburse {

        @ParameterizedTest(name = "distance={0}, rate={1}, expected={2}")
        @CsvSource({
            "10.0, 370, 37.00",
            "5.5,  370, 20.35",
            "0.0,  370, 0.00",
            "10.0, 200, 20.00",
        })
        void shouldCalculateAmountAsDistanceTimesRateDividedBy100(double distance, double rate, double expected) {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(distance)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .kmRate(rate)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert
            assertThat(result.get(0).getAmountToReimburse()).isCloseTo(expected, within(0.01));
        }
    }

    // -------------------------------------------------------------------------
    // processIndividualReport — CalculationType dispatch and distance logic
    // -------------------------------------------------------------------------
    @Nested
    class ProcessIndividualReport {

        @Test
        void shouldSetDistanceToRawDistanceForCalculatedWithoutExtraDistance() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(12.0)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert — no subtraction, distance = rawDistance
            assertThat(result.report().getDistance()).isEqualTo(12.0);
        }

        @Test
        void shouldDoubleDistanceForRoundTripCalculatedWithoutExtraDistance() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(6.0).roundTrip(true)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert
            assertThat(result.report().getDistance()).isEqualTo(12.0);
        }

        @Test
        void shouldSubtractHomeToWorkDistanceForCalculatedWhenStartsAtHome() {
            // Arrange — homeToWork=5km, route=10km, starts at home → subtract 5
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).startsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act — maxDistanceToSubtract=10 (5km * 2)
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert
            assertThat(result.report().getDistance()).isEqualTo(5.0);
        }

        @Test
        void shouldSubtractTwiceHomeToWorkWhenBothStartsAndEndsAtHome() {
            // Arrange — route=20km, homeToWork=5km; starts AND ends at home → subtract 10
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(20.0).startsAtHome(true).endsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act — maxDistanceToSubtract=10
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert — 20 - 10 = 10
            assertThat(result.report().getDistance()).isEqualTo(10.0);
        }

        @Test
        void shouldCapSubtractionAtMaxDistanceToSubtract() {
            // Arrange — toSubtract would be 10 (5km * 2) but max is only 6
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(20.0).startsAtHome(true).endsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 6.0);

            // Assert — capped at 6: 20 - 6 = 14
            assertThat(result.report().getDistance()).isEqualTo(14.0);
        }

        @Test
        void shouldReturnReducedRemainingMaxAfterSubtraction() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).startsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act — max=10, subtract=5
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert — 10 - 5 = 5 remaining
            assertThat(result.remainingMaxDistance()).isEqualTo(5.0);
        }

        @Test
        void shouldReturnZeroRemainingWhenMaxIsFullyConsumed() {
            // Arrange — starts AND ends at home, each consumes 5km; max=10 → fully consumed
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(20.0).startsAtHome(true).endsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert
            assertThat(result.remainingMaxDistance()).isEqualTo(0.0);
        }

        @Test
        void shouldMarkRecalculateAsFalseAfterProcessing() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .build();
            r.setRecalculate(true);

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert
            assertThat(result.report().isRecalculate()).isFalse();
        }

        @Test
        void shouldDetectStartsAtHomeFromGpsWhenNotFromApp() {
            // Arrange — home is at (56.0, 10.0); start coord is within 0.001 threshold
            when(addressService.getHomeAddressByDate(eq(person), any())).thenReturn(homeAddress);
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).fromApp(false)
                    .calculationType(CalculationType.CALCULATED)
                    .coords(List.of(
                            startCoord(56.0001, 10.0001),
                            endCoord(56.1, 10.1)))
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert — GPS proximity detected as home start
            assertThat(result.report().isStartsAtHome()).isTrue();
        }

        @Test
        void shouldNotDetectStartsAtHomeWhenGpsIsFarFromHome() {
            // Arrange — start coord is outside 0.001 threshold
            when(addressService.getHomeAddressByDate(eq(person), any())).thenReturn(homeAddress);
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).fromApp(false)
                    .calculationType(CalculationType.CALCULATED)
                    .coords(List.of(
                            startCoord(56.5, 10.5),
                            endCoord(56.1, 10.1)))
                    .build();

            // Act
            OnetimePaymentsCalculatorService.ProcessedReport result = service.processIndividualReport(r, 10.0);

            // Assert
            assertThat(result.report().isStartsAtHome()).isFalse();
        }

        @Test
        void shouldThrowForNullCalculationType() {
            // Arrange — null falls through the switch default
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .calculationType(CalculationType.READ)
                    .build();
            r.setCalculationType(null);

            // Act & Assert
            assertThatThrownBy(() -> service.processIndividualReport(r, 10.0))
                    .isInstanceOf(Exception.class);
        }
    }

    // -------------------------------------------------------------------------
    // calculate — redistribution and extra distance flag
    // -------------------------------------------------------------------------
    @Nested
    class Calculate {

        @Test
        void shouldNeverProduceNegativeDistanceOnAnyReport() {
            // Arrange — route shorter than homeToWork → would go negative without zeroing
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(3.0).startsAtHome(true)  // homeToWork=5 > rawDistance=3
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert — negative distances are zeroed
            assertThat(result).allMatch(rep -> rep.getDistance() >= 0.0);
        }

        @Test
        void shouldRedistributeNegativeOverflowToOtherReportsOnSameDay() {
            // Arrange — r1 route shorter than homeToWork so goes negative;
            // the overflow is redistributed to r2 by reducing its distance
            Report r1 = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(3.0).startsAtHome(true)
                    .status(ReportStatus.PENDING)
                    .calculationType(CalculationType.CALCULATED)
                    .build();
            Report r2 = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0)
                    .status(ReportStatus.PENDING)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act
            List<Report> result = calc(r1, r2);

            // Assert — total non-negative, no individual report negative
            double total = result.stream().mapToDouble(Report::getDistance).sum();
            assertThat(total).isGreaterThanOrEqualTo(0.0);
            assertThat(result).allMatch(rep -> rep.getDistance() >= 0.0);
        }

        @Test
        void shouldSetExtraDistanceFlagWhenCalculatedAndStartsAtHome() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0).startsAtHome(true)
                    .calculationType(CalculationType.CALCULATED)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert
            assertThat(result.get(0).isExtraDistance()).isTrue();
        }

        @Test
        void shouldNotSetExtraDistanceFlagForCalculatedWithoutExtraDistance() {
            // Arrange
            Report r = new ReportBuilder()
                    .person(person).employment(employment)
                    .rawDistance(10.0)
                    .calculationType(CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE)
                    .build();

            // Act
            List<Report> result = calc(r);

            // Assert
            assertThat(result.get(0).isExtraDistance()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // calculatePotentialDeltaDistance
    // -------------------------------------------------------------------------
    @Nested
    class CalculatePotentialDeltaDistance {

        private static final LocalDate DATE = LocalDate.of(2024, 11, 15);

        @Test
        void shouldReturnZeroWhenCalculationTypeIsNull() {
            double result = service.calculatePotentialDeltaDistance(
                    null, DATE, List.of(), 10.0, false, 10.0, 0, false, 2.0, false, 0);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        void shouldReturnZeroWhenLocalDateIsNull() {
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.CALCULATED, null, List.of(), 10.0, false, 10.0, 0, false, 2.0, false, 0);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        void shouldReturnMaxDistanceWhenNothingRemainsToSubtract() {
            // Arrange — alreadySubtracted == maxDistanceToSubtract
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.CALCULATED, DATE, List.of(), 10.0, false, 10.0, 10.0, false, 2.0, false, 0);

            assertThat(result).isEqualTo(10.0);
        }

        @Test
        void shouldReturnAlreadySubtractedWhenRouteHasNoHomeConnection() {
            // Arrange — READ type, startsOrEndsHomeForRead=false, no fourKmRule
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.READ, DATE, List.of(), 10.0, false, 10.0, 3.0, false, 2.0, false, 0);

            // Assert — no home connection so nothing new to subtract
            assertThat(result).isEqualTo(3.0);
        }

        @Test
        void shouldIncludeFourKmBorderDistanceAndRemainderWhenFourKmRuleEnabled() {
            // Arrange — READ, starts at home (startsOrEndsHomeForRead=true), fourKmRule,
            // homeToBorderDistance=2.0, fourKmWithdrawn=0
            // Expected: borderDistance(2) + fourKmRemainder(min(10-2, 4-0)=4) = 6
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.READ, DATE, List.of(), 10.0, true, 10.0, 0.0, true, 2.0, false, 0);

            assertThat(result).isCloseTo(6.0, within(0.01));
        }

        @Test
        void shouldDoubleBorderDistanceForRoundTripFourKmRule() {
            // Arrange — same as above but roundTrip=true → border doubles to 4, remainder=min(10-4,4)=4 → total 8
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.READ, DATE, List.of(), 10.0, true, 10.0, 0.0, true, 2.0, true, 0);

            assertThat(result).isCloseTo(8.0, within(0.01));
        }

        @Test
        void shouldSubtractFullRemainingAmountForReadWhenBothStartsAndEndsAtHome() {
            // Arrange — READ type uses startsOrEndsHomeForRead=true directly.
            // XOR=false (both home) → distanceToSubtract=min(10,10)=10; result = 0 + 10 = 10
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.READ, DATE, List.of(), 10.0, true, 10.0, 0.0, false, 2.0, false, 0);

            assertThat(result).isCloseTo(10.0, within(0.01));
        }

        @Test
        void shouldSubtractFullRemainingAmountForCalculatedWhenCoordsNearHome() {
            // Arrange — CALCULATED uses GPS coords + securityUtil.getPerson() to detect home proximity.
            // Both start and end coord placed at (56.0001, 10.0001) ≈ home (56.0, 10.0)
            when(securityUtil.getPerson()).thenReturn(person);
            when(addressService.getHomeAddressByDateCached(eq(person), any())).thenReturn(homeAddress);
            when(addressService.areAddressesCloseToEachOther(10.0, 56.0, 10.0001, 56.0001)).thenReturn(true);

            // Act — XOR=false (both near home) → distanceToSubtract=min(10,10)=10; result = 0 + 10 = 10
            double result = service.calculatePotentialDeltaDistance(
                    CalculationType.CALCULATED, DATE,
                    List.of(startCoord(56.0001, 10.0001), endCoord(56.0001, 10.0001)),
                    10.0, false, 10.0, 0.0, false, 2.0, false, 0);

            assertThat(result).isCloseTo(10.0, within(0.01));
        }
    }

    // -------------------------------------------------------------------------
    // getMaxDistanceToSubtractByAllEmployments
    // -------------------------------------------------------------------------
    @Nested
    class GetMaxDistanceToSubtractByAllEmployments {

        @Test
        void shouldReturnTwiceTheLargestHomeToWorkDistance() throws JsonProcessingException {
            // Arrange — emp1=5km, emp2=8km; max = 8 * 2 = 16
            Employment emp2 = Employment.builder().id(2L).person(person).build();
            Address work2 = new Address();
            work2.setType(AddressType.WORK);
            when(addressService.getWorkAddressByDate(eq(emp2), any())).thenReturn(work2);
            when(addressService.getHomeAddressByDate(eq(person), any())).thenReturn(homeAddress);
            when(routeService.distanceBetweenAddresses(homeAddress, work2)).thenReturn(8.0);

            // Act
            double result = service.getMaxDistanceToSubtractByAllEmployments(
                    List.of(employment, emp2), LocalDate.of(2024, 11, 15));

            // Assert
            assertThat(result).isEqualTo(16.0);
        }

        @Test
        void shouldReturnZeroForEmptyEmploymentList() {
            double result = service.getMaxDistanceToSubtractByAllEmployments(
                    List.of(), LocalDate.of(2024, 11, 15));

            assertThat(result).isEqualTo(0.0);
        }
    }
}
