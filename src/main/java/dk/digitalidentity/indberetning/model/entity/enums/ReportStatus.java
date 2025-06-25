package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("Afventer"),
    ACCEPTED("Godkendt"),
    REJECTED("Afvist"),
    REJECTED_AFTER_INVOICE("Afvist efter overførsel til løn"),
    INVOICED("Overført til løn"),
    API_READY("API"),
    API_FETCHED("API");

    private final String text;

    ReportStatus(String text) {
        this.text = text;
    }
}
