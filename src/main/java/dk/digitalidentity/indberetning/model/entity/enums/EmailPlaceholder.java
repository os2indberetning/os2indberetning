package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum EmailPlaceholder {
	EMPLOYEE_PLACEHOLDER("{modtager}", "Navnet på lederen/stedfortræderen der modtager mailen"),
	TIMESTAMP_PLACEHOLDER("{deadline}", "Tidsfristen angivet i deadline feltet");

	private String placeholder;
	private String description;

	private EmailPlaceholder(String placeholder, String description) {
		this.placeholder = placeholder;
		this.description = description;
	}
}
