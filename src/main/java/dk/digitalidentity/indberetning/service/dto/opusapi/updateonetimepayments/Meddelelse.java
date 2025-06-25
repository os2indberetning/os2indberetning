package dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments;

import lombok.Builder;

@Builder
public record Meddelelse(
		String Timestamp,
		int Kode,
		String Type,
		String Tekst
) { }
