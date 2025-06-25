package dk.digitalidentity.indberetning.model.entity.enums;

import lombok.Getter;

@Getter
public enum LogAction {
	KMD_OPUS_UPDATE(RetentionLevel.FOREVER),
	API_DRIVING_IMPORT(RetentionLevel.FOREVER),
	ACCEPT_REPORT(RetentionLevel.FOREVER),
	REJECT_REPORT(RetentionLevel.FOREVER),
	REJECT_APP_REPORT(RetentionLevel.FOREVER),
	API_UPDATE_ORG(RetentionLevel.FOREVER),
	CREATE_STANDARD_ADDRESS(RetentionLevel.FOREVER),
	UPDATE_STANDARD_ADDRESS(RetentionLevel.FOREVER),
	DELETE_STANDARD_ADDRESS(RetentionLevel.FOREVER),
	CHANGE_PRIME_ADDRESS(RetentionLevel.FOREVER),
	CREATE_OR_UPDATE_ONETIME_PAYMENT_REQ(RetentionLevel.FOREVER),
	ADDED_CMS_MESSAGE(RetentionLevel.FOREVER),
	UPDATE_ADMIN_EMAIL_FLAG(RetentionLevel.FOREVER);

	private final RetentionLevel retentionLevel;

	private LogAction(RetentionLevel retentionLevel) {
		this.retentionLevel = retentionLevel;
	}
}
