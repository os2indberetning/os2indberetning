package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.RateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateDao extends JpaRepository<Rate, Long> {
    Optional<Rate> findByActiveYearAndRateType(int activeYear, RateType rateType);

    List<Rate> findByActiveYear(int activeYear);
}
