package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.DeadlineEmails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeadlineDao extends JpaRepository<DeadlineEmails, Long> {
	List<DeadlineEmails> findAllByNextNotification(LocalDate nextNotification);
	List<DeadlineEmails> findByNextNotificationLessThanEqual(LocalDate nextNotification);


	DeadlineEmails findById(long id);

}
