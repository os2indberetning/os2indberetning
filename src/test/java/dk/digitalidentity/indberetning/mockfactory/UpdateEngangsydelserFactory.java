package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateEngangsydelser;

public class UpdateEngangsydelserFactory {

    public static UpdateEngangsydelser basic(long reportId, String antal) {
        return UpdateEngangsydelser.builder()
                .Startdato("2024-11-27T00:00:00Z")
                .Loenart("0100")
                .Loebenummer(1)
                .Sekvensnummer("0")
                .Antal(antal)
                .Afsendelses_System("OS2indberetning")
                .Indv_Bemaerkning(Long.toString(reportId))
                .reportId(reportId)
                .build();
    }
}
