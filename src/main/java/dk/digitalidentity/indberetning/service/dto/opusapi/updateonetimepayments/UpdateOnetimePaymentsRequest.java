package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import dk.digitalidentity.indberetning.service.dto.opusapi.Medarbejder;
import lombok.Builder;

import java.util.List;

@Builder
public record UpdateOnetimePaymentsRequest(
		UpdateRequestHeader Header,
		Medarbejder Medarbejder,
		List<UpdateEngangsydelser> Engangsydelser
) {}
