package dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments;

import lombok.Builder;

@Builder
public record VirkningFilter(String FraDatoTid, String TilDatoTid) {}
