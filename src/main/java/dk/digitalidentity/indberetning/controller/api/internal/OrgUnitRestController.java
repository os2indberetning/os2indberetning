package dk.digitalidentity.indberetning.controller.api.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;
import dk.digitalidentity.indberetning.model.entity.dto.RestOrgUnitDTO;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.service.OrgUnitService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api/orgUnit")
@NoRoleRequired
public class OrgUnitRestController {
	private final OrgUnitService orgUnitService;

	@PatchMapping("/{id}")
	@Transactional
	public ResponseEntity<?> updateOrgUnit(@PathVariable final long id, @RequestBody RestOrgUnitDTO dto) {
		final OrgUnit orgUnit = orgUnitService.findById(id);
		if(orgUnit == null) {
			return ResponseEntity.notFound().build();
		}

		orgUnitService.updateOrgUnit(id, dto);

		return ResponseEntity.ok().build();
	}
}
