package dk.digitalidentity.indberetning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.model.entity.Address;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import dk.digitalidentity.indberetning.model.entity.enums.CalculationType;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnetimePaymentsCalculatorService {

	private final AddressService addressService;
	private final OS2indberetningConfiguration configuration;
	private final ReportService reportService;
	private final RouteService routeService;
	private final SecurityUtil securityUtil;

	/**
	 * @param reports Must only contain reports from the same day
	 * @return
	 */
	public List<Report> calculate(List<Report> reports) {
		// Simple input validation, example: are all reports from the same day?
		validateReportInput(reports);

		// Find the maximum KM distance that we can subtract on the given day.
		// This is calculated as the biggest distance homeToWork that any of the used employments of the day has times two.
		// We cannot draw above this maximum, for any reason.
		LocalDate driveDate = !reports.isEmpty() ? reports.getFirst().getDriveDate() : null;
		Double maxDistanceToSubtract = getMaxDistanceToSubtract(reports, driveDate);

		// We sort the list so that the start and end point form a route regardless of when they were sent in.
		// This makes sure that the list is calculated in a consistent way
		ArrayList<Report> updatedReports = new ArrayList<>();
		for (Report rep : sortByRoute(reports)) {
			// Individual processing withdraws the initial amount to deduct by looking if the specific route starts or ends at home.
			// In that case this method draws the amount taking remaining maxDistance, homeToWork for the specified employment and other factors into account
			ProcessedReport processIndividualReport = processIndividualReport(rep, maxDistanceToSubtract);
			maxDistanceToSubtract = processIndividualReport.remainingMaxDistance;
			updatedReports.add(processIndividualReport.report);
		}

		// After processing the reports individually, we need to distribute any withdrawn kilometers along the route
		// in the case that more than the actual route length has been withdrawn from the route that starts or ends at home.
		// Here we skip any route we cannot use for this redistribution,
		// but otherwise we just move the withdrawn extra distance along the list of reports that we sorted earlier.
		Double totalAmountToRedistribute = updatedReports.stream().filter(report -> report.getDistance() < 0).map(Report::getDistance).reduce(Double::sum).orElse(0.0);
		if (totalAmountToRedistribute < 0.01) {
			for (Report updatedReport : updatedReports) {
				if (CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE.equals(updatedReport.getCalculationType()) ||
					!ReportStatus.PENDING.equals(updatedReport.getStatus()) ||
					updatedReport.isFourKmRule() ||
					updatedReport.getDistance() < 0.01 ||
					totalAmountToRedistribute > -0.01) {

					continue;
				}
				double amountToRedistribute = Double.min(Math.abs(totalAmountToRedistribute), updatedReport.getDistance());
				updatedReport.setDistance(updatedReport.getDistance() - amountToRedistribute);
				updatedReport.setExtraDistance(true);
				totalAmountToRedistribute = totalAmountToRedistribute + amountToRedistribute;
			}
		}

		// We are limited to withdraw at max the amount of kilometer actually driven,
		// so if any of the reports still has a negative distance that could not be redistributed we zero it.
		updatedReports.forEach(report -> report.setDistance(report.getDistance() < 0 ? 0 : report.getDistance()));
		updatedReports.stream().filter(report -> !CalculationType.CALCULATED_WITHOUT_EXTRA_DISTANCE.equals(report.getCalculationType())).forEach(report -> {
			double distance = report.getDistance();
			double rawDistance = report.getRawDistance();
			boolean roundTrip = report.isRoundTrip();
			int i = roundTrip ? 2 : 1;
			report.setExtraDistance((Math.abs((i * rawDistance) - distance)) >= 0.01);
		});


		// Applies the remainder of the four km rule by deducting the kilometers from the individual reports
		// and storing the new values in distance field.
		applyFourKmRule(updatedReports);

		// Update the amount to reimburse, which is i simple km to kr conversion using the given rate.
		// This is only used for reporting in the solution since we use the distance in KM when reporting to OPUS
		calculateAmountToReimburse(updatedReports);

		return updatedReports;
	}

	/**
	 * Takes a list of unsorted reports and sorts the ones we have GPS data for in order based on closeness of next starting point from the previous route
	 * @param unsorted
	 * @return
	 */
	private List<Report> sortByRoute(List<Report> unsorted) {
		//this sorts reports by matching coords endpoint with the (most) appropriate startpoint
		List<Report> sorted = new ArrayList<>();

		List<Report> readTypeReports = unsorted.stream().filter(report -> CalculationType.READ.equals(report.getCalculationType())).toList();
		unsorted.removeIf(report -> CalculationType.READ.equals(report.getCalculationType()));

		if (!unsorted.isEmpty()) {
			// TODO could this not return different starting points in the case of multiple startsAtHome reports on the same day?
			// 	That would potentially give differing route calculations between saves
			Report current = unsorted.stream().filter(Report::isStartsAtHome).findFirst().orElse(null);
			if (current == null) {
				current = unsorted.removeFirst();
			}
			else {
				unsorted.remove(current);
			}
			sorted.add(current);

			while (!unsorted.isEmpty()) {
				GpsCoordinate curCoord = current.getEndGps();
				Report next = unsorted.stream().min(Comparator.comparingDouble(rep -> calculateEuclidianDistance(rep.getStartGps(), curCoord))).orElse(unsorted.getFirst());
				unsorted.remove(next);
				sorted.add(next);
				current = next;
			}
		}

		if (!readTypeReports.isEmpty()) {
			sorted.addAll(readTypeReports);
		}
		return sorted;
	}

	private static double calculateEuclidianDistance(GpsCoordinate newCoord, GpsCoordinate curCoord) {
		if (newCoord == null || curCoord == null) {
			return Double.MAX_VALUE;
		}

		return Math.sqrt(
				Math.pow(newCoord.getLatitude() - curCoord.getLatitude(), 2) +
				Math.pow(newCoord.getLongitude() - curCoord.getLongitude(), 2)
		);
	}

	private static void validateReportInput(List<Report> reports) {
		if (reports == null || reports.isEmpty()) {
			throw new IllegalArgumentException("A list of Reports MUST be provided!");
		}

		LocalDate driveDate = null;
		Person person = null;
		for (Report report : reports) {
			if (driveDate == null) {
				driveDate = report.getDriveDate();
			}
			if (!Objects.equals(report.getDriveDate(), driveDate)) {
				throw new IllegalArgumentException("All DriveDates provided MUST be the same!");
			}

			if (person == null) {
				person = report.getPerson();
			}
			if (!Objects.equals(report.getPerson(), person)) {
				throw new IllegalArgumentException("All Reports MUST be from the same person!");
			}

		}
	}

	private List<Report> calculateAmountToReimburse(List<Report> updatedReports) {
		for (Report report : updatedReports) {
			double reimbursementCalculation = report.getDistance() * (report.getKmRate() / 100);
			report.setAmountToReimburse(DoubleUtil.round(reimbursementCalculation));
		}

		return updatedReports;
	}

	private void applyFourKmRule(List<Report> reports) {
		// We still need to deduct the 4 km rule individually from the reports
		// since we do not deduct an amount but rather an amount of KM
		// and since each report can have differing KM rates this is our only option

		int fourKmMax = 4;
		double distanceDeducted = 0;

		for (Report report : reports) {
			if (!report.isFourKmRule()) {
				continue;
			}

			if (distanceDeducted < fourKmMax) {
				if (report.getDistance() < fourKmMax - distanceDeducted) {
					// Deduct the full distance since it is smaller than the remainder of the 4km rule
					distanceDeducted += report.getDistance();
					report.setDistance(0d);
				}
				else {
					// Deduct the remainder of the 4km rule since the distance is bigger than it.
					report.setDistance(report.getDistance() - (fourKmMax - distanceDeducted));
					break; // Break since we have distributed the whole of the 4km rule at this point.
				}
			}
		}
 	}

	@Transactional
	public void recalculateReports() {
		List<Report> toBeRecalculated = reportService.getToBeRecalculated();

		if (log.isDebugEnabled()) {
			if (!toBeRecalculated.isEmpty()) {
				log.debug("Fetched: " + toBeRecalculated.size() + " to be recalculated");
			}
		}

		// Group reports by DriveDate for the recalculation
		Map<Person, Map<LocalDate, List<Report>>> reportMap = new HashMap<>();
		for (Report report : toBeRecalculated) {
			if (!reportMap.containsKey(report.getPerson())) {
				reportMap.put(report.getPerson(), new HashMap<>());
			}

			Map<LocalDate, List<Report>> reportByDateMap = reportMap.get(report.getPerson());
			if (!reportByDateMap.containsKey(report.getDriveDate())) {
				reportByDateMap.put(report.getDriveDate(), new ArrayList<>());
			}
			reportByDateMap.get(report.getDriveDate()).add(report);
		}

		int maxCalculationsPerRound = configuration.getScheduled().getRecalculateReportsMax();
		int currentCalc = 0;
		for (Map<LocalDate, List<Report>> reportsByDate : reportMap.values()) {
			ArrayList<Report> toBeSaved = new ArrayList<>();

			for (List<Report> reports : reportsByDate.values()) {
				if (currentCalc <= maxCalculationsPerRound) {
					List<Report> updatedReports = calculate(reports);
					currentCalc += updatedReports.size();
					toBeSaved.addAll(updatedReports);
				}
			}
			reportService.saveAll(toBeSaved);
		}
	}

	public record ProcessedReport(Report report, Double remainingMaxDistance) {}
	public ProcessedReport processIndividualReport(Report report, Double maxDistanceToSubstract) {

		// Figure out if the route starts or ends at home, this is important for multiple parts of the calculation.
		// This search is a little fuzzy since Apps might not return the exact same GPS position every time.
		// We assume that an Address within 100m of home in both the latitude and longitude directions is the home address.
		if (!CalculationType.READ.equals(report.getCalculationType()) && !report.isFromApp()) {
			// For CalculationType.READ the user inputs if they started/ended at home

			// Current active registered home address
			Address homeAddress = addressService.getHomeAddressByDate(report.getPerson(), report.getDriveDate().atStartOfDay());

			if (homeAddress == null) {
				log.warn("Home address was null PersonId:" + report.getPerson());
			}
			else {
				List<GpsCoordinate> coords = report.getCoords();
				GpsCoordinate start = coords.stream().filter(GpsCoordinate::isStartPoint).findAny().orElse(null);
				GpsCoordinate end = coords.stream().filter(GpsCoordinate::isEndPoint).findAny().orElse(null);
				if (start == null || end == null) {
					log.warn("GPS Coords missing!");
				}
				else {
					report.setStartsAtHome(areAddressesCloseToEachOther(homeAddress.getLongitude(), homeAddress.getLatitude(), start.getLongitude(), start.getLatitude()));
					report.setEndsAtHome(areAddressesCloseToEachOther(homeAddress.getLongitude(), homeAddress.getLatitude(),end.getLongitude(),end.getLatitude()));
				}
			}
		}

		// Reimbursement is not given for the distance to and from work, that is considered non-work related travel,
		// so we need to subtract this distance before calculating the amount to reimburse.
		//
		// The "4-km rule" can be used instead,
		// in that case we subtract the distance from home to the border of the municipality.
		double homeToWorkDistance = calculateDistanceBetweenHomeAndWork(report.getEmployment(), report.getDriveDate());
		double toSubtract = calculateDistanceToSubstract(report, homeToWorkDistance);

		// Calculate the distance to be reimbursed based on which CalculationType the user chose when creating the report
		//
		// All distances should be calculated from rawDistance OR routeInformation since the Distance field will be modified by several rules
		double distanceSubtracted = 0;
		switch (report.getCalculationType()) {
			case CALCULATED -> {
				distanceSubtracted = Double.min(toSubtract, report.isFourKmRule() ? toSubtract : maxDistanceToSubstract);
				calculateWithExtraDistance(report, distanceSubtracted);
			}
			case CALCULATED_WITHOUT_EXTRA_DISTANCE -> calculateWithoutExtraDistance(report);
			case READ -> {
				distanceSubtracted = Double.min(toSubtract, report.isFourKmRule() ? toSubtract : maxDistanceToSubstract);
				calculateByReadDistance(report, distanceSubtracted);
			}
			default -> throw new IllegalStateException("Unexpected value: " + report.getCalculationType());
		}

		// This will potentially return a negative value. This is intended as it just to calculate handle routes where first or last stop is smaller than the distance to work.
		// The calculate function handles that no negative distance is passed on beyond the previous mentioned calculations.
		report.setDistance(report.getDistance());

		report.setRecalculate(false);

		return new ProcessedReport(report, Double.max(0,maxDistanceToSubstract - distanceSubtracted));
	}

	/**
	 * Sets the distances based on our route calculation and subtracts the ExtraDistance if required.
	 *
	 * @param report
	 * @param toSubtract
	 * @return
	 */
	private Report calculateWithExtraDistance(Report report, double toSubtract) {
		if ((report.isStartsAtHome() || report.isEndsAtHome()) && !report.isFourKmRule()) {
			report.setExtraDistance(true);
		}
		double distance = report.isRoundTrip() ? report.getRawDistance() * 2 : report.getRawDistance();
		report.setDistance(distance - toSubtract);

		return report;
	}

	/**
	 * Sets the distances based on our route calculation with no km subtracted since no ExtraDistance has been specified.
	 * This additionally saves the specific route on the report as RouteGeometry
	 * @param report The report to be updated
	 * @return The updated report
	 */
	private Report calculateWithoutExtraDistance(Report report) {
		// No extra distance (merkørsel) has been specified
		report.setDistance(report.isRoundTrip() ? report.getRawDistance() * 2 : report.getRawDistance());
		return report;
	}

	/**
	 * Sets the distances based on the numbers reported by the user when reporting the drive
	 *
	 * @param report                 The report to be updated
	 * @param toSubtract             The distance to subtract from the self-reported distance (ExtraDistance)
	 * @return The updated report
	 */
	private Report calculateByReadDistance(Report report, double toSubtract) {
		if ((report.isStartsAtHome() || report.isEndsAtHome()) && !report.isFourKmRule()) {
			report.setExtraDistance(true);
		}
		// Save the self-reported raw distance before subtracting
		double distance = report.isRoundTrip() ? report.getRawDistance() * 2 : report.getRawDistance();
		report.setDistance(distance - toSubtract);
		return report;
	}

	private boolean areAddressesCloseToEachOther(double longA, double latA, double longB, double latB) {
		double coordinateThreshold = 0.001;
		double longDiff = Math.abs(longA - longB);
		double latDiff = Math.abs(latA - latB);

		return longDiff < coordinateThreshold && latDiff < coordinateThreshold;
	}

	/**
	 * Calculates the distance between home and work.
	 * If the user has provided a distance override on their employment we return that instead of calculating the route length
	 *
	 * @param employment The employment to use for the calculation
	 * @param driveDate
	 * @return The distance between the home and work addresses in km.
	 */
    public double calculateDistanceBetweenHomeAndWork(Employment employment, LocalDate driveDate) {
		// TODO: this will probably be deleted soon
		if (employment.getHomeToWorkDistanceOverride() != null) {
			return employment.getHomeToWorkDistanceOverride();
		}

		// We need to decide what to do with these addresses that turn up null,
		// alternatively we need to recalculate when an address turns up
		Address activeWorkAddress = addressService.getWorkAddressByDate(employment, driveDate.atStartOfDay());
		if (activeWorkAddress == null) {
			log.warn("The organisation {} does not have a work address associated with it for date {}", employment.getId(), driveDate);
			return 0;
		}

		if (AddressType.DWORK.equals(activeWorkAddress.getType()) &&
				activeWorkAddress.getDeviatingAddress() != null &&
				activeWorkAddress.getHomeToWorkDistanceOverrideDeviation() > 0.0) {
			// An overide distance has been provided for the home -> work route,
			// so we just return that value.
			return activeWorkAddress.getHomeToWorkDistanceOverrideDeviation();
		}

		Address activeHomeAddress = addressService.getHomeAddressByDate(employment.getPerson(), driveDate.atStartOfDay());
		if (activeHomeAddress == null) {
			log.warn("Employment: {} does not have an associated home address on date {}", employment.getId(), driveDate);
			return 0;
		}

		try {
			return routeService.distanceBetweenAddresses(activeHomeAddress, activeWorkAddress);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reimbursement is not given for the distance to and from work, that is considered non-work related travel,
	 * so we need to subtract this distance before calculating the amount to reimburse.
	 * <p>
	 * The "4-km rule" can be used instead,
	 * in that case we subtract the distance from home to the border of the municipality.
	 *
	 * @param report             used for the four km rule if applied
	 * @param homeToWorkDistance
	 * @return the amount of KM to subtract
	 */
	private double calculateDistanceToSubstract(Report report, double homeToWorkDistance) {
		double toSubtract = 0;
		if (report.isFourKmRule()) {
			double borderDistance = report.getHomeToBorderDistance();
			toSubtract += report.isStartsAtHome() ? borderDistance : 0;
			toSubtract += report.isEndsAtHome() ? borderDistance : 0;
		}
		else {
			toSubtract += report.isStartsAtHome() ? homeToWorkDistance : 0;
			toSubtract += report.isEndsAtHome() ? homeToWorkDistance : 0;
		}
		return toSubtract;
	}

	public double calculatePotentialDeltaDistance(CalculationType calculationType, LocalDate localDate, List<GpsCoordinate> coords, double distance, boolean startsOrEndsHomeForRead, double maxDistanceToSubtract, double alreadySubtracted, boolean fourKmRuleEnabled, double homeToBorderDistance, boolean roundTrip, double fourKmWithdrawn) {
		if (calculationType == null || localDate == null) {
			return 0;
		}

		double amountRemaining = maxDistanceToSubtract - alreadySubtracted;

		// If no amount is left to be taken from the drive date, we should simply return the max value
		if (amountRemaining == 0 || amountRemaining < 0) {
			return maxDistanceToSubtract;
		}

		StartsOrEndsHome startsOrEndsHome = isStartsOrEndsHome(calculationType, localDate, coords, startsOrEndsHomeForRead);
		if (startsOrEndsHome.startsHome || startsOrEndsHome.endsHome) {
			if (fourKmRuleEnabled) {
				double fourKmRemainderToSubtract = 0;
				if (fourKmRuleEnabled) {
					double distanceToSubtract = homeToBorderDistance;
					if (roundTrip) {
						distanceToSubtract = distanceToSubtract * 2;
					}

					distanceToSubtract = Double.min(distanceToSubtract, distance);

					if (distance - distanceToSubtract > 0.001 && fourKmWithdrawn < 4) {
						fourKmRemainderToSubtract = Double.min(distance - distanceToSubtract, 4 - fourKmWithdrawn);
					}
				}

				double distanceToSubtract = homeToBorderDistance;
				if (roundTrip) {
					distanceToSubtract = distanceToSubtract * 2;
				}

				distanceToSubtract = Double.min(distanceToSubtract, distance);
				distanceToSubtract += fourKmRemainderToSubtract;

				return switch (calculationType) {
					case CALCULATED, READ -> (alreadySubtracted + distanceToSubtract);
					default -> (alreadySubtracted + fourKmRemainderToSubtract);
				};
			}
			else {
				double distanceToSubtract = Double.min(distance,  (startsOrEndsHome.startsHome ^ startsOrEndsHome.endsHome) ? amountRemaining/2 : amountRemaining);
				if (roundTrip) {
					distanceToSubtract = distanceToSubtract * 2;
				}
				amountRemaining -= distanceToSubtract;

				return switch (calculationType) {
					case CALCULATED, READ -> amountRemaining < 0 ? maxDistanceToSubtract : (alreadySubtracted + distanceToSubtract);
					default -> alreadySubtracted;
				};
			}
		}
		else {
			return alreadySubtracted + (fourKmRuleEnabled ? Double.min(distance, 4 - fourKmWithdrawn) : 0);
		}
	}

	record StartsOrEndsHome(boolean startsHome, boolean endsHome) {}
	private StartsOrEndsHome isStartsOrEndsHome(CalculationType calculationType, LocalDate localDate, List<GpsCoordinate> coords, boolean startsOrEndsHomeForRead) {
		if (CalculationType.READ.equals(calculationType)) {
			return new StartsOrEndsHome(startsOrEndsHomeForRead, startsOrEndsHomeForRead);
		}

		boolean startsHome = false;
		boolean endsHome = false;
		if (!coords.isEmpty()) {
			Address homeAddress = addressService.getHomeAddressByDateCached(securityUtil.getPerson(), localDate.atStartOfDay());
			GpsCoordinate first = coords.stream().filter(GpsCoordinate::isStartPoint).findFirst().orElse(null);
			if (first != null && first.isStartPoint() && addressService.areAddressesCloseToEachOther(homeAddress.getLongitude(), homeAddress.getLatitude(), first.getLongitude(), first.getLatitude())) {
				startsHome = true;
			}

			GpsCoordinate last = coords.stream().filter(GpsCoordinate::isEndPoint).findFirst().orElse(null);
			if (last != null && last.isEndPoint() && addressService.areAddressesCloseToEachOther(homeAddress.getLongitude(), homeAddress.getLatitude(), last.getLongitude(), last.getLatitude())) {
				endsHome = true;
			}
		}
		return new StartsOrEndsHome(startsHome, endsHome);
	}

	public double getMaxDistanceToSubtract(List<Report> reports, LocalDate driveDate) {
       	// TODO verify how this works in regards to sorting
        return reports.stream()
                .map(Report::getEmployment)
                .filter(Objects::nonNull)
                .map(employment -> calculateDistanceBetweenHomeAndWork(employment, driveDate))
                .max(Comparator.naturalOrder())
                .orElse(0.0) * 2.0;
	}

	public double getMaxDistanceToSubtractByAllEmployments(List<Employment> employments, LocalDate driveDate) {
		return employments.stream()
				.filter(Objects::nonNull)
				.map(employment -> calculateDistanceBetweenHomeAndWork(employment, driveDate))
				.max(Comparator.naturalOrder())
				.orElse(0.0) * 2.0;
	}
}
