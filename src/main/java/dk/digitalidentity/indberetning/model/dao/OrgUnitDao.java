package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgUnitDao extends JpaRepository<OrgUnit, Long> {
    OrgUnit findByOrgId(String orgId);
    List<OrgUnit> findOrgUnitByLongDescriptionContaining(String input);
}
