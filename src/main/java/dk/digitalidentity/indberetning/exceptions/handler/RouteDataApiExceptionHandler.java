package dk.digitalidentity.indberetning.exceptions.handler;

import dk.digitalidentity.indberetning.controller.api.RouteDataAPIController;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.LocalDate;

@Slf4j
@RestControllerAdvice(assignableTypes = RouteDataAPIController.class)
public class RouteDataApiExceptionHandler {

	@ExceptionHandler(MissingPathVariableException.class)
	public ProblemDetail handleMissingParam(MissingPathVariableException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Required parameter '" + ex.getVariableName() + "' is missing");
		problem.setTitle("Missing Required Parameter");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String detail;
		if (ex.getRequiredType() != null && LocalDate.class.isAssignableFrom(ex.getRequiredType())) {
			detail = "Parameter '" + ex.getName() + "' must be a valid date (yyyy-MM-dd)";
		} else {
			detail = "Parameter '" + ex.getName() + "' has an invalid value";
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid Parameter");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid Parameter");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ProblemDetail handleRateLimitExceeded(RequestNotPermitted ex) {
		log.warn("Rate limit exceeded on route-data API: {}", ex.getMessage());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.TOO_MANY_REQUESTS,
				"Rate limit exceeded. Max 60 requests per minute.");
		problem.setTitle("Rate Limit Exceeded");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(BulkheadFullException.class)
	public ProblemDetail handleBulkheadFull(BulkheadFullException ex) {
		log.warn("Bulkhead full on route-data API: {}", ex.getMessage());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Service temporarily at capacity. Try again shortly.");
		problem.setTitle("Service Busy");
		problem.setType(URI.create("about:blank"));
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneral(Exception ex) {
		log.error("Unexpected error in route-data API", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An internal error occurred");
		problem.setTitle("Internal Server Error");
		problem.setType(URI.create("about:blank"));
		return problem;
	}
}
