package dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments;

import lombok.Builder;

@Builder
public record ReadOnetimePaymentsRequest(RequestHeader Header, dk.digitalidentity.indberetning.service.dto.opusapi.Medarbejder Medarbejder, VirkningFilter VirkningFilter) { }


