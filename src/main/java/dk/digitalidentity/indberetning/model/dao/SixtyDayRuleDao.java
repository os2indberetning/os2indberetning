package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.SixtyDayRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SixtyDayRuleDao extends JpaRepository<SixtyDayRule, Long> {
    List<SixtyDayRule> findByCountGreaterThanEqual(int count);

    List<SixtyDayRule> findByPersonAndAddress(Person person, String address);

    SixtyDayRule save(SixtyDayRule sixtyDayRule);

    List<SixtyDayRule> findByLastDriveDateBefore(LocalDate lastDriveDate);

    long deleteByLastDriveDateBeforeAllIgnoreCase(LocalDate lastDriveDate);
}
