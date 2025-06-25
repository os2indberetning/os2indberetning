package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import lombok.Builder;

@Builder
public record ResponseHeader(
		String Timestamp,
		String OpdateringsID,
		String Status
) { }
