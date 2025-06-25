package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum AddressType {
    HOME( "Hjemme adresse"),
    WORK( "Arbejdssted"),
    STANDARD( "Kommunal standardadresse"),
    ALTERNATIVE( "Alternativ adresse"),
    DHOME("Afvigende hjemadresse"),
    DWORK("Afvigende arbejdsadresse"),
    PERSONAL_ROUTE_POINT("Rutepunkt");

    private final String description;

    AddressType(String description) {
        this.description = description;
    }
}
