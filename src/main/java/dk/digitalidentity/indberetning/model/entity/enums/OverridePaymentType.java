package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum OverridePaymentType {
	NONE("Ingen afvigende kontering"),
	COST_CENTER("Afvigende omkostningssted"),
	PSP_ELEMENT("Afvigende PSP-element");

	private final String text;

	OverridePaymentType(String text) {
		this.text = text;
	}
}
