package dk.digitalidentity.indberetning.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.security.IdentityProviderProvider;
import dk.digitalidentity.indberetning.security.NoRoleRequired;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@NoRoleRequired
public class IdpDiscoveryController {
	private final OS2indberetningConfiguration configuration;

	private final IdentityProviderProvider identityProviderProvider;
	
	@GetMapping("/discovery")
	public String discovery(Model model) {
		if(configuration.skipDiscovery()) {
			return "redirect:/saml2/authenticate/" + identityProviderProvider.getIdentityProviders().getFirst().getLoginId();
		}

		model.addAttribute("identityProviders", identityProviderProvider.getIdentityProviderMap());
		return "discovery";
	}

}
