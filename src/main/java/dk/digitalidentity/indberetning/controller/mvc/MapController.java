package dk.digitalidentity.indberetning.controller.mvc;

import com.google.maps.model.EncodedPolyline;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.Roles;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AddressService;
import dk.digitalidentity.indberetning.service.EmploymentService;
import dk.digitalidentity.indberetning.service.GpsCoordinateService;
import dk.digitalidentity.indberetning.service.ReportService;
import dk.digitalidentity.indberetning.service.RouteService;
import dk.digitalidentity.indberetning.service.SubstituteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class MapController {
	private final AddressService addressService;
	private final GpsCoordinateService gpsCoordinateService;
	private final ReportService reportService;
	private final RouteService routeService;
	private final SecurityUtil securityUtil;
	private final SubstituteService substituteService;
	private final EmploymentService employmentService;

	@GetMapping("/approve/mapFragment/{id}")
	public String  mapFragment(Model model, @PathVariable(name = "id") long id){
		Report report = reportService.getById(id);
		if (!securityUtil.isAdmin() && !Objects.equals(securityUtil.getPerson(), report.getPerson())) {
			if (securityUtil.hasRole(Roles.ROLE_APPROVER)) {

				Set<Employment> employments = employmentService.getByIdIn(substituteService.whoDoILeadOrSub(securityUtil.getPerson(), LocalDate.now()));
				boolean matches = employments.stream().anyMatch(employment -> Objects.equals(employment.getEmployeeNumber(), report.getEmployeeNumber()));
				if (!matches) {
					return "error";
				}
			}
		}

		EncodedPolyline encPoly = new EncodedPolyline(report.getRoute().getRouteGeometry());
		String decodedRoute = routeService.convertGoogleLatLngToGeoJsonRoute(encPoly.decodePath());
		model.addAttribute("report", report);
		model.addAttribute("route", decodedRoute);
		model.addAttribute("distance", report.isRoundTrip() ? report.getRawDistance() * 2 : report.getRawDistance());
		model.addAttribute("interestpoints", gpsCoordinateService.getByReport(report));
		model.addAttribute("primaryAddress", addressService.getPrimary());
		return "approve/mapFragment";
	}
}
