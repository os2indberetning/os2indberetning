package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum CalculationType {
    CALCULATED("1", "Beregnet"),
    READ("2", "Aflæst"),
    CALCULATED_WITHOUT_EXTRA_DISTANCE("3", "Beregnet uden merkørsel");

    private final String key;
    private final String message;

    CalculationType(String key, String message) {
        this.key = key;
        this.message = message;
    }
}