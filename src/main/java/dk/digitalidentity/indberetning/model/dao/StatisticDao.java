package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Statistic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatisticDao extends JpaRepository<Statistic, Long> {
    Statistic findByFunctionName(String functionName);
}
