package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.ApiAccess;
import dk.digitalidentity.indberetning.model.entity.enums.ApiType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiAccessDao extends JpaRepository<ApiAccess, Long> {

	Optional<ApiAccess> findFirstByTypeAndDisabledFalse(ApiType type);
}