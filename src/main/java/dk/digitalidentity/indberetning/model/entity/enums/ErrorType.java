package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum ErrorType {
    INFO("I", "Info"),
    WARNING("W", "Advarsel"),
    ERROR("E", "Fejl");

    private final String abbreviation;
    private final String description;

    ErrorType(String abbreviation, String description) {
        this.abbreviation = abbreviation;
        this.description = description;
    }

    public static ErrorType fromAbbreviation(String abbreviation) {
        for (ErrorType type : values()) {
            if (type.getAbbreviation().equalsIgnoreCase(abbreviation)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with abbreviation " + abbreviation);
    }
}
