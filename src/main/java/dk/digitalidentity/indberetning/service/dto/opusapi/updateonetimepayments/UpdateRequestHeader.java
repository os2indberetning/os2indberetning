package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import lombok.Builder;

@Builder
public record UpdateRequestHeader(
		String ClientID,
		String OpdateringsID,
		String Kun_Valider
) {}

