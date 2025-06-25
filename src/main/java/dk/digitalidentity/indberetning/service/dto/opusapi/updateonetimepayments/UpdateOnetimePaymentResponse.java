package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import dk.digitalidentity.indberetning.service.dto.opusapi.Medarbejder;

import java.util.List;

public record UpdateOnetimePaymentResponse(ResponseHeader Header, Medarbejder Medarbejder, List<Meddelelse> Meddelelse) {
}
