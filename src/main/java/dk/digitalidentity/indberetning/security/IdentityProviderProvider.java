package dk.digitalidentity.indberetning.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.security.enums.IdentityProviderType;
import dk.digitalidentity.samlmodule.model.IdentityProvider;
import dk.digitalidentity.samlmodule.model.SamlIdentityProviderProvider;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IdentityProviderProvider implements SamlIdentityProviderProvider {
	private final Map<IdentityProviderType, IdentityProvider> idps;

	IdentityProviderProvider(final OS2indberetningConfiguration configuration) {
		this.idps = configuration.getIdps().entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> new IdentityProvider(
						entry.getValue().getEntityId(),
						entry.getValue().getMetadataUrl(),
						"")));
	}

	@Override
	public IdentityProvider getByEntityId(final String entityId) {
		if (entityId == null) {
			throw new IllegalStateException();
		}

		return idps.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getEntityId().equals(entityId))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
	}

	@Override
	public List<IdentityProvider> getIdentityProviders() {
		return idps.entrySet().stream().map(Map.Entry::getValue).toList();
	}

	public Map<IdentityProviderType, IdentityProvider> getIdentityProviderMap() {
		return idps;
	}
}
