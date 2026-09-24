package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.Engangsydelser;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.ReadOnetimePaymentsResponse;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.ResponseHeader;

import java.util.List;

public class ReadOnetimePaymentsResponseFactory {

    public static ReadOnetimePaymentsResponse success(List<Engangsydelser> payments) {
        return new ReadOnetimePaymentsResponse(new ResponseHeader(0, null), null, payments);
    }

    public static ReadOnetimePaymentsResponse successWithTwoPayments() {
        return success(List.of(
                EngangsydelserFactory.os2("2024-11-27", "42", 0.25),
                EngangsydelserFactory.os2("2024-11-27", "42", -0.25)
        ));
    }

    // Status=1, Tekst="Data ikke fundet." — the API's empty-result convention
    public static ReadOnetimePaymentsResponse notFound() {
        return new ReadOnetimePaymentsResponse(new ResponseHeader(1, "Data ikke fundet."), null, null);
    }

    public static ReadOnetimePaymentsResponse error(int status, String tekst) {
        return new ReadOnetimePaymentsResponse(new ResponseHeader(status, tekst), null, null);
    }

    public static ReadOnetimePaymentsResponse noHeader() {
        return new ReadOnetimePaymentsResponse(null, null, null);
    }
}
