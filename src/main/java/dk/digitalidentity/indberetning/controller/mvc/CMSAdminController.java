package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.cms.CmsMessageBundle;
import dk.digitalidentity.indberetning.service.cms.CmsMessageService;
import lombok.RequiredArgsConstructor;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;

@Controller
@RequiredArgsConstructor
@RequireAdministrator
public class CMSAdminController {

	private final CmsMessageBundle cmsMessageBundle;
	private final CmsMessageService cmsMessageService;

	record CmsMessage(String key, String value, String description) {}
	@GetMapping("/admin/cms-edit")
	public String editCms(Model model, @RequestParam("key") String key) {
		CmsMessage dto = new CmsMessage(key, cmsMessageBundle.getText(key, true), cmsMessageBundle.getDescription(key));
		model.addAttribute("cmsMessage", dto);
		return "admin/cms/edit";
	}

	@PostMapping("/admin/cms-edit")
	public String saveCms(Model model, CmsMessage cmsMsg) {
		if (cmsMsg.value.length() > 65536) {
			CmsMessage dto = new CmsMessage(cmsMsg.key, cmsMsg.value, cmsMessageBundle.getDescription(cmsMsg.key));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);

			return "admin/cms/edit";
		}

		PolicyFactory policy = new HtmlPolicyBuilder()
				.allowCommonBlockElements()
				.allowCommonInlineFormattingElements()
				.allowElements("a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "target").onElements("a")
				.allowAttributes("class", "style", "id", "name").globally()
				.toFactory();
		String safeHTML = policy.sanitize(cmsMsg.value);

		dk.digitalidentity.indberetning.model.entity.CmsMessage cms = cmsMessageService.getByCmsKey(cmsMsg.key);
		if (cms == null) {
			cms = new dk.digitalidentity.indberetning.model.entity.CmsMessage();
			cms.setCmsKey(cmsMsg.key);
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(cms);

		return "redirect:/admin#cms";
	}

	@PostMapping("/admin/cms-edit/logo")
	public String saveCms(@RequestParam("value") MultipartFile file, @RequestParam("key") String key, Model model) throws IOException {
		String base64 = Base64.getEncoder().encodeToString(file.getBytes());

		dk.digitalidentity.indberetning.model.entity.CmsMessage logo = cmsMessageService.getByCmsKey("cms.logo");

		if (base64.length() > 65536) {
			CmsMessage dto = new CmsMessage(logo.getCmsKey(), null, cmsMessageBundle.getDescription(logo.getCmsKey()));
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);

			return "admin/cms/edit";
		}

		if (logo == null) {
			logo = new dk.digitalidentity.indberetning.model.entity.CmsMessage();
			logo.setCmsKey("cms.logo");
		}

		logo.setCmsValue(base64);
		logo.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(logo);

		return "redirect:/admin#cms";
	}

	@PostMapping("rest/cms/delete/{id}")
	public ResponseEntity<String> deleteStandardAddress(@PathVariable String id) {
		dk.digitalidentity.indberetning.model.entity.CmsMessage cms = cmsMessageService.getByCmsKey(id);

		if (cms != null) {
			// Instead of deleting we set it to null as it is easier to update across servers
			cms.setCmsValue(null);
			cms.setLastUpdated(LocalDateTime.now());
			cmsMessageService.save(cms);
		}
		return ResponseEntity.ok().build();
	}
}
