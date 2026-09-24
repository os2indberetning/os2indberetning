package dk.digitalidentity.indberetning.mockfactory;

import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.Engangsydelser;

public class EngangsydelserFactory {

    public static Engangsydelser os2(String startdato, String reportId, double antal) {
        return new Engangsydelser(startdato, "OBJ001", "0100", 1, 0.0, antal, 0, null, "OS2indberetning", reportId);
    }

    public static Engangsydelser external(String startdato) {
        return new Engangsydelser(startdato, "OBJ999", "0100", 1, 0.0, 1.0, 0, null, "ExternalSystem", null);
    }
}
