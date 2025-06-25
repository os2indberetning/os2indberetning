package dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments;

import lombok.Builder;

@Builder
public record Engangsydelser(String Startdato, String Objekt_id, String Loenart, int Loebenummer, double Beloeb, double Antal, int Afv_Timeloen, String Afv_PSP_Element, String Indv_Bemaerkning_1, String Indv_Bemaerkning_2) {}
