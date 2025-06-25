package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.RateType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateTypeDao extends JpaRepository<RateType, Long> {
    RateType findByName(String name);
    RateType findByPrimeTrue();
}
