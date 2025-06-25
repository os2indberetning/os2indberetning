package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum CmsProperty {
	LOGO(								"cms.logo", 									"Her kan du uploade et logo, som præsenteres for alle brugerne i øverste venstre hjørne. Det er vigtigt at logoet er i PNG format, og har en gennemsigtig baggrund."),
	FOOTER_TEXT(						"cms.footer.text", 							"Tekst der fremgår i bunden af alle skærmbilleder, kan bruges til at indsætte \"Information og vejledning\"-link."),

	FOUR_KM_RULE_HELP_TEXT(				"cms.help.four.km.rule", 						"Hjælpetekst der vises ved afkrydsningsfeltet for 4-km reglen."),
	FOUR_KM_RULE_VALUE_HELP_TEXT(		"cms.help.four.km.rule.value", 				"Hjælpetekst der vises ved indtastning af afstand til kommunegrænse, når 4-km reglen er i brug."),
	SIXTY_DAYS_RULE_HELP_TEXT(			"cms.help.sixty.day.rule", 					"Hjælpetekst der vises i forbindelse med 60-dages reglen"),
	EXTRA_DISTANCE_HELP_TEXT(			"cms.help.extra.distance", 					"Hjælpetekst der vises i forbindelse med det totale fratrukkede merkørslesfradrag for en given kørselsdato"),

	MOBILE_TOKEN_HELP_TEXT(				"cms.help.mobile.token", 						"Hjælpetekst der vises ved indstillingerne for App adgang."),

	PRIMARY_LICENSE_PLATE_HELP_TEXT(	"cms.help.license.plate.primary", 				"Hjælpetekst der vises ved indstillingerne for primær nummerplade."),
	NO_LICENSE_PLATE_HELP_TEXT(			"cms.help.license.plate.not.registered", 		"Hjælpetekst der vises hvis ingen nummerplade er registreret"),

	ALTERNATIVE_WORK_DISTANCE_HELP_TEXT("cms.help.alternative.home.to.work.distance",	"Hjælpetekst der vises ved indstillingerne for brugerdefineret indtastning af afstand til arbejde."),
	ALTERNATIVE_HOME_ADDRESS_HELP_TEXT(	"cms.help.alternative.address.home", 			"Hjælpetekst der vises ved indstillingerne for afvigende hjemmeadresse."),
	ALTERNATIVE_WORK_ADDRESS_HELP_TEXT(	"cms.help.alternative.address.work", 			"Hjælpetekst der vises ved indstillingerne for afvigende arbejdsadresse."),

	PURPOSE_HELP_TEXT(					"cms.help.report.purpose", 					"Hjælpetekst der vises ved \"formål\"-feltet når man indberetter en kørsel."),
	TERMINATED_HELP_TEXT(					"cms.help.sub.terminated", 					"Hjælpetekst der vises ved stedfortræder/godkender, hvor en eller flere personer er fratrådt"),
	READ_REPORT_COMMENT_HELP_TEXT(		"cms.help.report.comment", 					"Hjælpetekst der vises ved oprettelsen af en rapport til yderligere bemærkninger om den kørte rute"),
	PERSONAL_APPROVER_HELP_TEXT(		"cms.help.personal.approver", 					"Hjælpetekst der vises ved oprettelse af en personlig godkender."),

	EMAIL_DEFAULT_SUBJECT(				"cms.help.email.deadline.subject", 			"Standard tekst: emnefelt som bruges til email adviseringer", true),
	EMAIL_DEFAULT_MESSAGE(				"cms.help.email.deadline.body", 				"Standard tekst: besked som bruges til email adviseringer", true),
	NOTIFICATION_MESSAGE(				"cms.notification.body", 				"Standard tekst: som vises som notifikation for alle bruger"),

	ADDRESSWASH_ADDRESS_TO_MAP(			"cms.help.address.wash.address.to.map", 		"Hjælpetekst der vises på adresse til kort knappen i adressevask"),
	ADDRESSWASH_MAP_TO_ADDRESS(         "cms.help.address.wash.map.to.address",     	"Hjælpetekst der vises på kort til adresse knappen i adressevask");

	private final String key;
	private final String description;
	private final boolean simple;

	private CmsProperty(String key, String description) {
		this.key = key;
		this.description = description;
		this.simple = false;
	}

	private CmsProperty(String key, String description, boolean simple) {
		this.key = key;
		this.description = description;
		this.simple = simple;
	}

	public static CmsProperty findByKey(String key) {
		for (CmsProperty value : CmsProperty.values()) {
			if (value.getKey().equalsIgnoreCase(key)) {
				return value;
			}
		}
		return null;
	}
}
