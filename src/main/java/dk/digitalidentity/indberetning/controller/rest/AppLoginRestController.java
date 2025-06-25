package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.entity.AppLogin;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.AppLoginService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@NoRoleRequired
@RequiredArgsConstructor
public class AppLoginRestController {
	private final SecurityUtil securityUtil;
	private final AppLoginService appLoginService;
    private final BCryptPasswordEncoder encoder;

	public record AppLoginRecord(long id, String username, String password, String passwordConfirm) {
	}

	//Create mapping
	@PostMapping("/rest/appLogin/create")
	public ResponseEntity<String> createAppLogin(@RequestBody AppLoginRecord appLogRec) {
		if (!(appLogRec.password).equals(appLogRec.passwordConfirm) || appLogRec.password.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

        Person person = securityUtil.getPerson();
        if (appLoginService.getByPersonId(person.getId()) != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		AppLogin appLog = new AppLogin();
		appLog.setUsername(appLogRec.username);
        appLog.setPassword(encoder.encode(appLogRec.password));
		appLog.setPerson(person);

		appLoginService.save(appLog);
		return ResponseEntity.ok().build();
	}

	//Delete mapping
	@PostMapping("/rest/appLogin/delete/{appLogId}")
	public ResponseEntity<String> deleteAppLogin(@PathVariable("appLogId") long appLogId) {
		if (appLoginService.getById(appLogId).getPerson() != securityUtil.getPerson()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

        appLoginService.delete(appLogId);
		return ResponseEntity.ok().build();
	}

	//Update mapping
	@PostMapping("/rest/appLogin/update")
	public ResponseEntity<String> updateAppLogin(@RequestBody AppLoginRecord appLogRec) {
		if (!(appLogRec.password).equals(appLogRec.passwordConfirm) || appLogRec.password.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

        AppLogin appLog = appLoginService.getById(appLogRec.id);
		if (appLog == null) {
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}

		if (appLog.getPerson() != securityUtil.getPerson()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		appLog.setUsername(appLogRec.username);
        appLog.setPassword(encoder.encode(appLogRec.password));

		appLoginService.save(appLog);
		return ResponseEntity.ok().build();
	}
}
