package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.AppLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppLoginDao extends JpaRepository<AppLogin, Long> {
    AppLogin findById(long id);
    Optional<AppLogin> findByUuid(String uuid);
    Optional<AppLogin> findByUsername(String username);
    AppLogin findByPersonId(long id);
}
