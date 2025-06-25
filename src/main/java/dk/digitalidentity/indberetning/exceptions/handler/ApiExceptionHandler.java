package dk.digitalidentity.indberetning.exceptions.handler;

import dk.digitalidentity.indberetning.controller.api.InternalAPIController;
import dk.digitalidentity.indberetning.exceptions.NotCommittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@Slf4j
@ControllerAdvice(assignableTypes = InternalAPIController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(NotCommittedException.class)
    @ResponseBody
    public ResponseEntity<?> handleInvalidRequestException(final NotCommittedException ex, final HttpServletRequest request) {
        return new ResponseEntity<>("The following changes would have been made in the DB:" + ex.getMessage(), HttpStatus.OK);
    }

}
