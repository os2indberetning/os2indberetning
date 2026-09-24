package dk.digitalidentity.indberetning.service;

import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.Route;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteDataExportService {

	private static final Set<ReportStatus> APPROVED_STATUSES = Set.of(
			ReportStatus.INVOICED,
			ReportStatus.ACCEPTED
	);

	private final ReportService reportService;
	private final RouteService routeService;
	private final OrgUnitService orgUnitService;
	private final EmploymentService employmentService;

	// ── DTOs ────────────────────────────────────────────────────────────

	public record RouteDataPageDTO(
			List<RouteDataReportDTO> content,
			int page,
			int size,
			long totalElements,
			int totalPages) {}

	public record RouteDataReportDTO(
			long id,
			LocalDate driveDate,
			LocalDateTime createdDate,
			String purpose,
			double distance,
			double rawDistance,
			boolean roundTrip,
			boolean fromApp,
			boolean startsAtHome,
			boolean endsAtHome,
			String calculationType,
			String status,
			String employeeNumber,
			long orgUnitId,
			String orgUnitName,
			double kmRate,
			String kmRateType,
			String routeGeometryWkt,
			Double estimatedDuration,
			Double actualDuration,
			List<RouteDataGpsPointDTO> gpsPoints) {}

	public record RouteDataGpsPointDTO(
			double latitude,
			double longitude,
			LocalDateTime createdAt,
			int pointNumber,
			boolean waypoint,
			boolean startPoint,
			boolean endPoint,
			String address,
			boolean returnLeg) {}

	public record RouteDataOrgUnitDTO(
			long id,
			String orgId,
			String name,
			Long parentId,
			List<RouteDataAddressDTO> workAddresses) {}

	public record RouteDataAddressDTO(
			String streetName,
			String streetNumber,
			int zipCode,
			String town,
			double latitude,
			double longitude) {}

	public record RouteDataEmploymentDTO(
			long id,
			String employeeNumber,
			String position,
			long orgUnitId,
			String orgUnitName) {}


	@Transactional(readOnly = true)
	public RouteDataPageDTO getReports(LocalDate from, LocalDate to, Long orgUnitId, int page, int size) {
		Page<Report> reportPage = reportService.getByStatusesAndDriveDateBetween(
				APPROVED_STATUSES, from, to, orgUnitId,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "driveDate")));

		List<Report> reports = reportPage.getContent();

		// Batch-fetch Route entities for reports that have one (avoids N+1 on lazy Route)
		Map<Long, Route> routesByReportId = batchFetchRoutes(reports);

		List<RouteDataReportDTO> content = reports.stream()
				.map(report -> toReportDTO(report, routesByReportId.get(report.getId())))
				.toList();

		return new RouteDataPageDTO(
				content,
				reportPage.getNumber(),
				reportPage.getSize(),
				reportPage.getTotalElements(),
				reportPage.getTotalPages());
	}

	@Transactional(readOnly = true)
	public Set<RouteDataOrgUnitDTO> getAllOrgUnits() {
		return orgUnitService.getAll().stream()
				.map(ou -> new RouteDataOrgUnitDTO(
						ou.getId(),
						ou.getOrgId(),
						ou.getLongDescription(),
						ou.getParent() != null ? ou.getParent().getId() : null,
						toAddressDTOs(ou)))
				.collect(Collectors.toSet());
	}

	@Transactional(readOnly = true)
	public List<RouteDataEmploymentDTO> getAllEmployments(boolean activeOnly) {
		List<Employment> data = activeOnly
				? employmentService.findByStopDateAfterOrStopDateNull(LocalDateTime.now())
				: employmentService.findAll();

		return data.stream()
				.map(e -> new RouteDataEmploymentDTO(
						e.getId(),
						e.getEmployeeNumber(),
						e.getPosition(),
						e.getOrgUnit().getId(),
						e.getOrgUnit().getLongDescription()))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private Map<Long, Route> batchFetchRoutes(List<Report> reports) {
		List<Long> reportIds = reports.stream().map(Report::getId).toList();

		// Find which reports actually have a route_id set
		Set<Long> routeReportIds = reportService.getRouteReportIds(reportIds);
		if (routeReportIds.isEmpty()) {
			return Map.of();
		}

		// Batch-load Route entities via the reports that have them;
		// since Report.route is lazy @OneToOne, we go through the reports we know have routes
		// and access the route — Hibernate will batch these if batch_size is configured,
		// but to be safe we use RouteService.findAllById with the route IDs directly.
		// We need to get route IDs from the reports themselves.
		List<Long> routeIds = reports.stream()
				.filter(r -> routeReportIds.contains(r.getId()))
				.map(r -> r.getRoute().getId())
				.toList();

		Map<Long, Route> routesById = routeService.findAllById(routeIds).stream()
				.collect(Collectors.toMap(Route::getId, Function.identity()));

		// Map back to report ID → Route
		return reports.stream()
				.filter(r -> r.getRoute() != null && routesById.containsKey(r.getRoute().getId()))
				.collect(Collectors.toMap(Report::getId, r -> routesById.get(r.getRoute().getId())));
	}

	// ── Mapping helpers ─────────────────────────────────────────────────

	private RouteDataReportDTO toReportDTO(Report report, Route route) {
		String wkt = null;
		Double estimatedDuration = null;
		if (route != null) {
			if (route.getRouteGeometry() != null && !route.getRouteGeometry().isBlank()) {
				wkt = encodedPolylineToWkt(route.getRouteGeometry());
			}
			if (route.getEstimatedTravelTime() != null) {
				estimatedDuration = report.isRoundTrip()
						? route.getEstimatedTravelTime() * 2
						: route.getEstimatedTravelTime();
			}
		}

		// Coords are already JOIN FETCHed — no extra query
		List<RouteDataGpsPointDTO> gpsPoints = report.getCoords().stream()
				.map(gps -> new RouteDataGpsPointDTO(
						gps.getLatitude(),
						gps.getLongitude(),
						gps.getCreatedAt(),
						gps.getPointNumber(),
						gps.isWaypoint(),
						gps.isStartPoint(),
						gps.isEndPoint(),
						gps.getAddress(),
						false))
				.toList();

		// Derive actualDuration from start/end GPS timestamps if both are present.
		// For round trips, the first GPS point has both startPoint and endPoint flags set to the
		// same coordinate, so we resolve endTime from the highest pointNumber instead — the JS
		// always places the turnaround arrival time (waypointTimeEnd) at the last address index.
		// The full round-trip duration is doubled from the one-way leg (symmetric assumption).
		Double actualDuration = null;
		LocalDateTime startTime = gpsPoints.stream()
				.filter(RouteDataGpsPointDTO::startPoint)
				.map(RouteDataGpsPointDTO::createdAt)
				.filter(Objects::nonNull)
				.findFirst().orElse(null);
		LocalDateTime endTime;
		if (report.isRoundTrip()) {
			endTime = gpsPoints.stream()
					.max(Comparator.comparingInt(RouteDataGpsPointDTO::pointNumber))
					.map(RouteDataGpsPointDTO::createdAt)
					.filter(Objects::nonNull)
					.orElse(null);
		} else {
			endTime = gpsPoints.stream()
					.filter(RouteDataGpsPointDTO::endPoint)
					.map(RouteDataGpsPointDTO::createdAt)
					.filter(Objects::nonNull)
					.findFirst().orElse(null);
		}
		if (startTime != null && endTime != null && endTime.isAfter(startTime)) {
			long seconds = Duration.between(startTime, endTime).getSeconds();
			actualDuration = report.isRoundTrip() ? (double) seconds * 2 : (double) seconds;
		}

		// For round trips, append synthetic return-leg points when all forward timestamps are present.
		// Return time for each point = turnaroundTime + (turnaroundTime - forwardPoint.createdAt),
		// which mirrors the forward journey symmetrically.
		List<RouteDataGpsPointDTO> timeline = new ArrayList<>(gpsPoints);
		if (report.isRoundTrip() && gpsPoints.size() >= 2) {
			List<RouteDataGpsPointDTO> sorted = gpsPoints.stream()
					.sorted(Comparator.comparingInt(RouteDataGpsPointDTO::pointNumber))
					.toList();
			boolean allTimesPresent = sorted.stream().allMatch(p -> p.createdAt() != null);
			if (allTimesPresent) {
				LocalDateTime turnaroundTime = sorted.getLast().createdAt();
				int syntheticPointNumber = gpsPoints.size();
				for (int i = sorted.size() - 2; i >= 0; i--) {
					RouteDataGpsPointDTO fwd = sorted.get(i);
					LocalDateTime returnTime = turnaroundTime.plus(Duration.between(fwd.createdAt(), turnaroundTime));
					boolean isReturnEnd = (i == 0);
					timeline.add(new RouteDataGpsPointDTO(
							fwd.latitude(),
							fwd.longitude(),
							returnTime,
							syntheticPointNumber++,
							!isReturnEnd,
							false,
							isReturnEnd,
							fwd.address(),
							true));
				}
			}
		}

		// Employment + OrgUnit already JOIN FETCHed — no extra query
		OrgUnit orgUnit = report.getEmployment().getOrgUnit();

		return new RouteDataReportDTO(
				report.getId(),
				report.getDriveDate(),
				report.getCreatedDate(),
				report.getPurpose(),
				report.getDistance(),
				report.getRawDistance(),
				report.isRoundTrip(),
				report.isFromApp(),
				report.isStartsAtHome(),
				report.isEndsAtHome(),
				report.getCalculationType() != null ? report.getCalculationType().name() : null,
				report.getStatus().name(),
				report.getEmployeeNumber(),
				orgUnit.getId(),
				orgUnit.getLongDescription(),
				report.getKmRate(),
				report.getKmRateType(),
				wkt,
				estimatedDuration,
				actualDuration,
				timeline);
	}

	private List<RouteDataAddressDTO> toAddressDTOs(OrgUnit ou) {
		return ou.getAddresses().stream()
				.map(a -> new RouteDataAddressDTO(
						a.getStreetName(),
						a.getStreetNumber(),
						a.getZipCode(),
						a.getTown(),
						a.getLatitude(),
						a.getLongitude()))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private String encodedPolylineToWkt(String encoded) {
		List<LatLng> points = routeService.decodePolyline(new EncodedPolyline(encoded));
		if (points.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder("LINESTRING(");
		for (int i = 0; i < points.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			// WKT coordinate order: longitude latitude
			sb.append(points.get(i).lng).append(" ").append(points.get(i).lat);
		}
		sb.append(")");
		return sb.toString();
	}
}
