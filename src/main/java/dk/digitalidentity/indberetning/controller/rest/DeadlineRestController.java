package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.entity.DeadlineEmails;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.DeadlineEmailsService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Objects;

@Hidden
@Slf4j
@RestController
@RequireAdministrator
@RequiredArgsConstructor
public class DeadlineRestController {
	private final DeadlineEmailsService deadlineEmailsService;

	public record DeadlineRecord(Long deadlineId,
								 @NotBlank(message = "Skal have et emne") String subject,
								 @NotBlank(message = "Skal have en besked") String msg,
								 LocalDate deadline,
								 @NotNull(message = "En dato for første afsendelse skal angives") @FutureOrPresent(message = "Dato skal være i dag eller derefter") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate first,
								 @NotNull Boolean repeat) { }
	@PostMapping(path = "rest/deadline/create")
	ResponseEntity<?> deleteDeadlineNotification(@Valid @RequestBody DeadlineRecord deadlineRecord, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body(bindingResult.getAllErrors().get(0));
		}

		DeadlineEmails deadlineEmails = new DeadlineEmails();
		if (deadlineRecord.deadlineId != null && deadlineRecord.deadlineId > 0) {
			deadlineEmails = deadlineEmailsService.getById(deadlineRecord.deadlineId);
		}
		// Ensuring new lines are accounted for
		String message = deadlineRecord.msg();
		message = message.replaceAll("\n", "<br>");
		deadlineEmails.setMessage(message);

		deadlineEmails.setSubject(deadlineRecord.subject);
		deadlineEmails.setDeadline(deadlineRecord.deadline);
		deadlineEmails.setFirstNotification(deadlineRecord.first);
		deadlineEmails.setRepeating(deadlineRecord.repeat);
		deadlineEmails.setNextNotification(deadlineEmails.getFirstNotification());

		deadlineEmailsService.save(deadlineEmails);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(path = "rest/deadline/delete/{id}")
	ResponseEntity<?> deleteDeadlineNotification(@PathVariable long id) {
		DeadlineEmails deadlineEmails = deadlineEmailsService.getById(id);
		if(Objects.equals(null, deadlineEmails)) {
			return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		deadlineEmailsService.removeDeadline(deadlineEmails);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
