package dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments;

import lombok.Builder;

@Builder
public record ResponseHeader(int Status, String Tekst) {}
