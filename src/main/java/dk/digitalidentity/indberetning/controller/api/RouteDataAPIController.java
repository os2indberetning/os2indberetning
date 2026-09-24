package dk.digitalidentity.indberetning.controller.api;

import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.RouteDataExportService;
import dk.digitalidentity.indberetning.service.RouteDataExportService.RouteDataEmploymentDTO;
import dk.digitalidentity.indberetning.service.RouteDataExportService.RouteDataOrgUnitDTO;
import dk.digitalidentity.indberetning.service.RouteDataExportService.RouteDataPageDTO;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@NoRoleRequired
@RequestMapping("/ext/api/routedata")
@RequiredArgsConstructor
public class RouteDataAPIController {

	private static final String BACKEND_NAME = "routedata";
	private static final int MAX_PAGE_SIZE = 200;

	private final RouteDataExportService routeDataExportService;

	@Operation(
			summary = "Get paginated reports",
			description = "Returns approved (INVOICED or ACCEPTED) reports within the given date range. Optionally filter by org unit.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Reports returned successfully"),
					@ApiResponse(responseCode = "400", description = "Invalid date range or page size"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "429", description = "Rate limit exceeded")
			}
	)
	@GetMapping("/reports")
	@RateLimiter(name = BACKEND_NAME)
	@Bulkhead(name = BACKEND_NAME)
	public ResponseEntity<RouteDataPageDTO> getReport(
			@RequestParam LocalDate from,
			@RequestParam LocalDate to,
			@RequestParam(required = false) Long orgUnitId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		if (size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must not exceed " + MAX_PAGE_SIZE);
		}

		if (from.isAfter(to)) {
			throw new IllegalArgumentException("From date cannot be after to date");
		}

		if (from.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("From date cannot be in the future. No reports are found");
		}

		return ResponseEntity.ok(routeDataExportService.getReports(from, to, orgUnitId, page, size));
	}

	@Operation(
			summary = "Get all org units",
			description = "Returns all org units with their work addresses.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Org units returned successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "429", description = "Rate limit exceeded")
			}
	)
	@GetMapping("/orgunits")
	@RateLimiter(name = BACKEND_NAME)
	@Bulkhead(name = BACKEND_NAME)
	public ResponseEntity<Set<RouteDataOrgUnitDTO>> getAllOrgUnits() {
		return ResponseEntity.ok(routeDataExportService.getAllOrgUnits());
	}

	@Operation(
			summary = "Get all employments",
			description = "Returns all employments. Defaults to active employments only.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Employments returned successfully"),
					@ApiResponse(responseCode = "401", description = "Unauthorized"),
					@ApiResponse(responseCode = "429", description = "Rate limit exceeded")
			}
	)
	@GetMapping("/employments")
	@RateLimiter(name = BACKEND_NAME)
	@Bulkhead(name = BACKEND_NAME)
	public ResponseEntity<List<RouteDataEmploymentDTO>> getAllEmployments(
			@RequestParam(defaultValue = "true") boolean activeOnly) {
		return ResponseEntity.ok(routeDataExportService.getAllEmployments(activeOnly));
	}
}
