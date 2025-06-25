package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.emailService.service.EmailQueueService;
import dk.digitalidentity.indberetning.model.dao.DeadlineDao;
import dk.digitalidentity.indberetning.model.entity.DeadlineEmails;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.EmailPlaceholder;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineEmailsService {
	private final DeadlineDao deadlineDao;
	private final PersonService personService;
	private final EmailQueueService emailQueueService;
	private final ReportService reportService;
	private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
			.toFormatter(Locale.ENGLISH);
	private final AddressService addressService;

	public DeadlineEmails getById(long id) {
		return deadlineDao.findById(id);
	}
	
	public List<DeadlineEmails> getAll() {
		return deadlineDao.findAll();
	}
	
	public List<DeadlineEmails> getTodaysNotifications() {
		List<DeadlineEmails> deadlines = deadlineDao.findByNextNotificationLessThanEqual(LocalDate.now());

		// Lets through any who has never been sent or is repeating
		return deadlines.stream()
				.filter(deadline -> deadline.getLastSent() == null || deadline.isRepeating())
				.toList();
	}
	
	public DeadlineEmails save(DeadlineEmails deadlineEmails) {
		return deadlineDao.save(deadlineEmails);
	}
	
	public void removeDeadline(DeadlineEmails deadlineEmails) {
		deadlineDao.delete(deadlineEmails);
	}

	@Transactional
	public void processDeadlineEmails() {
		List<DeadlineEmails> deadlineEmails = getTodaysNotifications();

		Set<Person> receivers = new HashSet<>();
		int emailsGenerated = 0;
		if (!deadlineEmails.isEmpty()) {

			List<Report> byStatusPending = reportService.getByStatus(ReportStatus.PENDING);
			receivers = reportService.findAllApproversForReports(byStatusPending);

			for (DeadlineEmails deadline : deadlineEmails) {
				for (Person receiver : receivers) {
					if (!StringUtils.hasLength(receiver.getEmail()) || !receiver.isReceiveEmail() || !receiver.isReceiveApproverMail()) {
						continue;
					}
					String subject = cleanStringOfPlaceholders(deadline.getSubject(), deadline, receiver);
					String message = cleanStringOfPlaceholders(deadline.getMessage(), deadline, receiver);
					message = message.replaceAll("\n", "<br>");
					emailQueueService.queueEmail(receiver.getEmail(), subject, message);
					emailsGenerated++;
				}

				deadline.setLastSent(LocalDate.now());
				if (deadline.isRepeating()) {
					LocalDate nextDate = deadline.getNextNotification().plusMonths(1);
					deadline.setNextNotification(nextDate);
				}
				save(deadline);
			}
		}

		int deadlinesDue = deadlineEmails.size();
		int receiversCount = receivers.size();
		log.info("Finished creating email queues for deadlines. deadlinesDue = " + deadlinesDue + ", receiversCount = " + receiversCount + ", emailsGenerated = " + emailsGenerated);
	}

	public void sendEmailsToAdmins(String subject, String message) {
		boolean notWashedAdressesExists = addressService.getDirtyAddresses().stream().anyMatch(address -> !address.washed());
		if (notWashedAdressesExists) {
			for (Person admin : personService.getAllAdmins()) {
				if (admin.isReceiveEmail() && admin.isReceiveAdminMail() && StringUtils.hasText(admin.getEmail())) {
					emailQueueService.queueEmail(admin.getEmail(), subject, message);
				}
			}
		}
	}

	@NotNull
	private String cleanStringOfPlaceholders(String text, DeadlineEmails deadline, Person receiver) {
		text = text.replace(EmailPlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), receiver.getName());

		if (deadline.getDeadline() != null) {
			text = text.replace(EmailPlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), formatter.format(deadline.getNextNotification().withDayOfMonth(deadline.getDeadline().getDayOfMonth())));
		}
		return text;
	}
}
