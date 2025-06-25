package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.ErrorResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorResponseDao extends JpaRepository<ErrorResponse, Long> {
}
