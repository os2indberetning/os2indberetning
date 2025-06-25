package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.SAMLException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Slf4j
@Controller
@NoRoleRequired
@RequiredArgsConstructor
public class DefaultController implements ErrorController {

	private final SecurityUtil securityUtil;
	private final OS2indberetningConfiguration configuration;
	private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

	@GetMapping("/")
	public String index() {
		if (securityUtil.isAuthenticated()) {
			if (configuration.isAllowNewReports()) {
				return "redirect:/report";
			}
			else {
				return "redirect:/report/list";
			}
		}
		return "index";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
        if (securityUtil.isAuthenticated()) {
			if (configuration.isAllowNewReports()) {
				return "redirect:/report";
			}
			else {
				return "redirect:/report/list";
			}
		}
        return "index";
    }

	@RequestMapping(value = "/error", produces = "text/html")
	public String handleError(Model model, HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		// deal with SAML errors first
		Object status = body.get("status");
		if (status instanceof Integer && (Integer) status == 999) {
			Object authException = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

			// handle the forward case
			if (authException == null && request.getSession() != null) {
				authException = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
			}

			if (authException instanceof Throwable t) {
				StringBuilder builder = new StringBuilder();

				logThrowable(builder, t, false);
				model.addAttribute("exception", builder.toString());

				if (t.getCause() != null) {
					t = t.getCause();

					// deal with the known causes for this error
					if (t instanceof SAMLException) {
						if (t.getCause() instanceof CredentialsExpiredException) {
							model.addAttribute("cause", "EXPIRED");
						}
						else if (t.getMessage() != null && t.getMessage().contains("Response issue time is either too old or with date in the future")) {
							model.addAttribute("cause", "SKEW");
						}
						else if (t.getMessage() != null && t.getMessage().contains("urn:oasis:names:tc:SAML:2.0:status:Responder")) {
							model.addAttribute("cause", "RESPONDER");
						}
						else {
							model.addAttribute("cause", "UNKNOWN");
						}
					}
					else {
						model.addAttribute("cause", "UNKNOWN");
					}
					log.info("Cause +  " + model.getAttribute("cause"));
				}

				log.info("exception = " + model.getAttribute("exception"));

				return "error/saml";
			}
		}

		// default to ordinary error message in case error is not SAML related
		model.addAllAttributes(body);

		return "error/default";
	}

	private Map<String, Object> getErrorAttributes(WebRequest request) {
		return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
	}

	private void logThrowable(StringBuilder builder, Throwable t, boolean append) {
		StackTraceElement[] stackTraceElements = t.getStackTrace();

		builder.append((append ? "Caused by: " : "") + t.getClass().getName() + ": " + t.getMessage() + "\n");
		for (int i = 0; i < 5 && i < stackTraceElements.length; i++) {
			builder.append("  ... " + stackTraceElements[i].toString() + "\n");
		}

		if (t.getCause() != null) {
			logThrowable(builder, t.getCause(), true);
		}
	}

	@GetMapping("/error/unknown-user")
	public String municipalityNotAdded() {
		return "error/unknown-user";
	}

}
