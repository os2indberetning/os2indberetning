package dk.digitalidentity.indberetning.config.settings.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IdentityProviderConfiguration {
	private String entityId;
	private String metadataUrl;
}
