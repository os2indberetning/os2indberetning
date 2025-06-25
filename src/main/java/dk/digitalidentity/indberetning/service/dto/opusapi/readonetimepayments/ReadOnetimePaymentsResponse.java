package dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments;

import lombok.Builder;

import java.util.List;

@Builder
public record ReadOnetimePaymentsResponse(ResponseHeader Header, dk.digitalidentity.indberetning.service.dto.opusapi.Medarbejder Medarbejder, List<Engangsydelser> Engangsydelser) {
}

