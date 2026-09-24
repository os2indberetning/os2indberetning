package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.OrgUnit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrgUnitDao extends JpaRepository<OrgUnit, Long> {
    OrgUnit findByOrgId(String orgId);
    List<OrgUnit> findOrgUnitByLongDescriptionContaining(String input);

	@Query("SELECT DISTINCT o FROM OrgUnit o LEFT JOIN FETCH o.addresses")
	List<OrgUnit> findAllWithAddresses();
}
