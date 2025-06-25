package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder
public record UpdateEngangsydelser(
		String Startdato,
		String Loenart,
		int Loebenummer,
		String Sekvensnummer,
		String Antal,
		String Afv_Omkostningssted,
		String Afv_PSP_Element,
		String Afsendelses_System,
		String Indv_Bemaerkning,

		@JsonIgnore
		long reportId
) {}
