package dk.digitalidentity.indberetning.config.settings.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduledConfiguration {
	private boolean enabled = true; // Master switch

	private boolean deadlineEmailsEnabled = true;
	private String deadlineEmailsCron;

	private boolean exportOnetimePaymentsEnabled = true;
	private String exportOnetimePaymentsCron;

	private boolean processAddressesEnabled = true;
	private String processAddressesCron;

	private boolean sixtyDayRuleEnabled = true;
	private String sixtyDayRuleCron;
	private String sixtyDayRuleResetCron;

	private boolean updateApproversForPendingReportsEnabled = true;
	private String updateApproversForPendingReportsCron;

	private boolean recalculateReportsEnabled = true;
	private boolean recalculateReportsCron;
	private int recalculateReportsMax = 200;

	private boolean sendRejectedEmailsEnabled = true;
	private String sendRejectedEmailsCron;

	private boolean addressWashEmailsEnabled = true;
	private String addressWashEmailsCron;

	private boolean substituteChangeListenerEnabled = true;
	private String substituteChangeListenerCron;

	private boolean updateStatisticsEnabled = true;
	private String updateStatisticsCron;

	private boolean checkForTerminationsTaskEnabled = true;
	private String checkForTerminationsTaskCron;

	private boolean checkForOpusDataEnabled = true;
	private String checkForOpusDataCron;

	private boolean cleanupOldAuditlogEnabled = true;
	private String cleanupOldAuditlogCron;
}
