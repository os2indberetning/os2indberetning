package dk.digitalidentity.indberetning.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressWashEmailConfiguration {
	private String defaultSubject = "Der er uvaskede adresser i adressevask";
	private String defaultMessage = "Der er uvaskede adresser i OS2indberetning. Disse adresser kan føre til fejl i udregninger.";
}
