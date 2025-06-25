package dk.digitalidentity.indberetning.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeadlineEmailConfiguration {
	private String defaultSubject = "Du har indberetninger af kørsel der afventer din godkendelse";
	private String defaultMessage = "Du har indberetninger af kørsel, der afventer din godkendelse i OS2Indberetning \n\n Fristen for godkendelse til næste lønudbetaling er d. {deadline}";
}
