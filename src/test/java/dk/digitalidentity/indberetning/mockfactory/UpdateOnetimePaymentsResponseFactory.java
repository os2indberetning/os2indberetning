package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.Meddelelse;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.ResponseHeader;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateOnetimePaymentResponse;

import java.util.List;

public class UpdateOnetimePaymentsResponseFactory {

    public static UpdateOnetimePaymentResponse success() {
        return new UpdateOnetimePaymentResponse(new ResponseHeader(null, null, "OK"), null, List.of());
    }

    public static UpdateOnetimePaymentResponse error(List<Meddelelse> messages) {
        return new UpdateOnetimePaymentResponse(new ResponseHeader(null, null, "Error"), null, messages);
    }

    public static Meddelelse errorMessage(int kode, String tekst) {
        return new Meddelelse(null, kode, "E", tekst);
    }

    public static Meddelelse warningMessage(int kode, String tekst) {
        return new Meddelelse(null, kode, "W", tekst);
    }
}
