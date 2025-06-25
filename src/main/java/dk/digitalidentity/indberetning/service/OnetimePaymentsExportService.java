package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.exceptions.KMDOnetimeException;
import dk.digitalidentity.indberetning.model.entity.ErrorComment;
import dk.digitalidentity.indberetning.model.entity.ErrorLog;
import dk.digitalidentity.indberetning.model.entity.ErrorResponse;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ErrorType;
import dk.digitalidentity.indberetning.model.entity.enums.LogAction;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.dto.opusapi.Medarbejder;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.Engangsydelser;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.ReadOnetimePaymentsRequest;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.ReadOnetimePaymentsResponse;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.RequestHeader;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.VirkningFilter;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateEngangsydelser;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateOnetimePaymentResponse;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateOnetimePaymentsRequest;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateRequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

@Slf4j
@Service
public class OnetimePaymentsExportService {

	public static final DateTimeFormatter YEAR_AND_MONTH_FORMATTER = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append(DateTimeFormatter.ofPattern("yyyy-MM"))
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
			.toFormatter(Locale.ENGLISH);
	private final ErrorCommentService errorCommentService;
	private final ErrorLogService errorLogService;
	private final ErrorResponseService errorResponseService;
	private final RestClient restClient;
	private final OS2indberetningConfiguration configuration;
	private final ReportService reportService;
	private final AuditLogService auditLogService;

	public OnetimePaymentsExportService(@Qualifier("opusRestClient") RestClient restClient, OS2indberetningConfiguration configuration, ReportService reportService, ErrorLogService errorLogService, ErrorResponseService errorResponseService, AuditLogService auditLogService, SecurityUtil securityUtil, ErrorCommentService errorCommentService) {
		this.errorCommentService = errorCommentService;
		this.errorLogService = errorLogService;
		this.errorResponseService = errorResponseService;
		this.restClient = restClient;
		this.configuration = configuration;
		this.reportService = reportService;
		this.auditLogService = auditLogService;
	}

	public List<Engangsydelser> readOnetimePayments(String employeeNumber, LocalDateTime fromDate, LocalDateTime toDate) throws KMDOnetimeException {
		String fromDateStr = fromDate != null ? fromDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT) : "";
		String toDateStr = toDate != null ? toDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT) : "";

		ReadOnetimePaymentsRequest requestBody = ReadOnetimePaymentsRequest.builder()
				.Header(new RequestHeader(configuration.getOpus().getMunicipalityCode()))
				.Medarbejder(new Medarbejder(employeeNumber))
				.VirkningFilter(new VirkningFilter(fromDateStr, toDateStr))
				.build();

		if (log.isTraceEnabled()) {
			log.trace("ReadOnetimePaymentsRequest = " + requestBody);
		}

		try {
			ReadOnetimePaymentsResponse body = restClient
					.post()
					.uri(configuration.getOpus().getApiBaseUrl() + "lpe/read-onetime-payments")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						if (log.isDebugEnabled()) {
							byte[] errorBody = StreamUtils.copyToByteArray(response.getBody());
							if (errorBody.length > 0) {
								log.debug("ReadOnetimePayments: Response body = {}", new String(errorBody, StandardCharsets.UTF_8));
							}
						}
						throw new KMDOnetimeException(response.getStatusText());
					})
					.body(ReadOnetimePaymentsResponse.class);

			if (body == null) {
				String msg = "ReadOnetimePayments: Empty body in response!";
				log.warn(msg);
				throw new KMDOnetimeException(msg);
			}
			if (log.isDebugEnabled()) {
				log.debug("body = " + body);
			}

			if (body.Header() == null) {
				throw new KMDOnetimeException("No header");
			}

			if (body.Header().Status() != 0 && (body.Header().Status() != 1 || !Objects.equals(body.Header().Tekst(), "Data ikke fundet."))) {
				throw new KMDOnetimeException(body.Header().Tekst());
			}

			return body.Engangsydelser();
		}
		catch (HttpClientErrorException e) {
			log.info("e.getStatusCode() = " + e.getStatusCode());
		}
		return null;
	}

	public boolean updateOnetimePayments(String employeeNumber, List<UpdateEngangsydelser> onetimePayments) {

		UpdateRequestHeader.UpdateRequestHeaderBuilder builder = UpdateRequestHeader.builder();
		builder.ClientID(configuration.getOpus().getMunicipalityCode());
		builder.OpdateringsID(UUID.randomUUID().toString().replace("-", "").toUpperCase()); // This is how KMD expect UUIDs, might be worth remembering this value for debugging
		if (configuration.getOpus().isOnlyValidateUpdates()) {
			// Adding this will let us validate that the payment will be accepted without actually adding it to the list of payments in KMD.
			// Nice for use with a dry-run/no export toggle
			builder.Kun_Valider("1");
		}
		UpdateRequestHeader header = builder.build();

		Medarbejder medarbejder = new Medarbejder(employeeNumber);
		ArrayList<UpdateEngangsydelser> updatedPayments = new ArrayList<>();
		for (int i = 0; i < onetimePayments.size(); i++) {
			UpdateEngangsydelser payment = onetimePayments.get(i);
			updatedPayments.add(updateSequenceNumber(payment, i));
		}
		
		UpdateOnetimePaymentsRequest requestBody = UpdateOnetimePaymentsRequest.builder()
				.Header(header)
				.Medarbejder(medarbejder)
				.Engangsydelser(updatedPayments)
				.build();
		
		auditLogService.saveSystem(LogAction.CREATE_OR_UPDATE_ONETIME_PAYMENT_REQ, "", requestBody);
		if (log.isDebugEnabled()) {
			log.debug("UpdateOnetimePaymentsRequest: " + requestBody);
		}

		try {
			UpdateOnetimePaymentResponse body = restClient
					.post()
					.uri(configuration.getOpus().getApiBaseUrl() + "lpe/update-onetime-payments")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						if (log.isDebugEnabled()) {
							byte[] errorBody = StreamUtils.copyToByteArray(response.getBody());
							if (errorBody.length > 0) {
								log.debug("UpdateOnetimePayments: Response body = {}", new String(errorBody, StandardCharsets.UTF_8));
							}
						}
						throw new KMDOnetimeException(response.getStatusText());
					})
					.body(UpdateOnetimePaymentResponse.class);

			if (body == null) {
				String msg = "UpdateOnetimePayments: Empty body in response!";
				log.warn(msg);
				throw new KMDOnetimeException(msg);
			}

			if ("Error".equals(body.Header().Status())) {
				StringBuilder sb = new StringBuilder();
				body.Meddelelse().stream()
						.filter(meddelelse -> "E".equals(meddelelse.Type()))
						.map(meddelelse -> meddelelse.Tekst() + " (Kode: " + meddelelse.Kode() + ")")
						.forEach(s -> sb.append(s).append("\n"));

				body.Meddelelse().forEach(meddelelse -> {
					ErrorLog errorLog = new ErrorLog();
					errorLog.setCreationDate(LocalDateTime.now());
					errorLog.setErrorCode(meddelelse.Kode());
					errorLog.setErrorText(meddelelse.Tekst());
					errorLog.setErrorType(ErrorType.fromAbbreviation(meddelelse.Type()));
					errorLog.setEmailSent(false);

					ErrorResponse errRep = new ErrorResponse();
					errRep.setResponse(body.toString());
					errorLog.setErrorResponse(errRep);
					errorResponseService.save(errRep);

					UpdateEngangsydelser failingPayment = updatedPayments.stream()
							.filter(updateEngangsydelser -> matchOnetimePaymentToError(meddelelse.Tekst(), updateEngangsydelser.Sekvensnummer()))
							.findFirst()
							.orElse(null);

					if(failingPayment != null) {
						errorLog.setReport(reportService.getById(failingPayment.reportId()));
						errorLog = fixErrorUpdateOnetimePayments(errorLog);
					}

					errorLogService.save(errorLog);
				});
				log.warn("UpdateOnetimePayments: response unsuccessful = \n" + sb);
				return false;
			}

			if (log.isDebugEnabled()) {
				log.debug("body = " + body);
			}

			return !configuration.getOpus().isOnlyValidateUpdates();
		}
		catch (HttpClientErrorException e) {
			log.info("e.getStatusCode() = " + e.getStatusCode());
			return false;
		}
	}

	private static boolean matchOnetimePaymentToError(String errorMessage, String sequenceNumber) {
		// Safety checks
		if (!StringUtils.hasLength(errorMessage) || !errorMessage.startsWith("Engangsydelse Nr. ") || errorMessage.length() < 20) {
			return false;
		}

		// Actual parsing we need this since OPUS puts lead zeros on their sequence numbers in the response.
		// TODO: Maybe we should add lead zeroes to our messages to begin with, if allowed
		String substring = errorMessage.substring(18, 21);
		int parsedInt = parseInt(substring);
		String parsedIntString = String.valueOf(parsedInt);

		return parsedIntString.equals(sequenceNumber);
	}

	private ErrorLog fixErrorUpdateOnetimePayments(ErrorLog errorLog) {
		Report erroneousReport = errorLog.getReport();
		StringBuilder sb = new StringBuilder();
		sb.append("OS2Indberetning er stødt på en fejl i indberetning af følgende rapport:\nid: ");
		sb.append(erroneousReport.getId());
		sb.append("\nIndberetter: ");
		sb.append(erroneousReport.getFullName());
		sb.append("\nDato for kørsel: ");
		sb.append(erroneousReport.getDriveDate());
		sb.append("\n\nSystemet har fået på følgende fejl: ");
		sb.append(errorLog.getErrorText());
		sb.append("\n");

		boolean reportUpdated = false;
		switch (errorLog.getErrorCode()) {
			case 119 -> { // 119 Angiv gyldigt omkostningssted.
				sb.append("Omkostningssted");
				sb.append(" (").append(StringUtils.hasLength(erroneousReport.getOverrideCostCenter()) ? erroneousReport.getOverrideCostCenter() : "feltet er tomt").append(") ");
				sb.append("er ikke gyldigt.");

				errorLog.setAssignedTo(erroneousReport.getApprovedBy());
				if (configuration.getOpus().isCorrectInvalidOnetimePayments()) {
					erroneousReport.setStatus(ReportStatus.PENDING);
					erroneousReport.setOverrideCostCenter(null);
					erroneousReport.setApprovedBy(null);
					reportUpdated = true;
				}
			}
			case 138 ->  // 138 Medarbejderen er låst
				sb.append("Medarbejderen er låst. Ingen handling påkrævet, rapport genindsendes.");
			case 151 -> { // 151 PSP element & er ikke gyldigt.
				sb.append("PSP element ");
				sb.append(" (").append(StringUtils.hasLength(erroneousReport.getOverridePspElement()) ? erroneousReport.getOverridePspElement() : "feltet er tomt").append(") ");
				sb.append("er ikke gyldigt.");

				errorLog.setAssignedTo(erroneousReport.getApprovedBy());
				if (configuration.getOpus().isCorrectInvalidOnetimePayments()) {
					erroneousReport.setStatus(ReportStatus.PENDING);
					erroneousReport.setOverridePspElement(null);
					erroneousReport.setApprovedBy(null);
					reportUpdated = true;
				}
			}
			case 167 -> { // 167 Du må ikke anvende både afv. omkostningssted og afv. PSP-element.
				sb.append("Du må ikke anvende både afv. omkostningssted og afv. PSP-element.");

				if (configuration.getOpus().isCorrectInvalidOnetimePayments()) {
					erroneousReport.setOverridePspElement(null);
					reportUpdated = true;
				}
			}
			default -> sb.append("Systemet har ikke foretaget sig ydeligere");
		}

		if (reportUpdated) {
			reportService.save(erroneousReport);
		}

		ErrorComment errC = new ErrorComment();
		errC.setComment(sb.toString());
		errorCommentService.save(errC);
		errorLog.setComment(errC);
		return errorLog;
	}

	private static UpdateEngangsydelser updateSequenceNumber(UpdateEngangsydelser payment, int i) {
		return UpdateEngangsydelser.builder()
				.Startdato(payment.Startdato())
				.Loenart(payment.Loenart())
				.Loebenummer(payment.Loebenummer())
				.Antal(payment.Antal())
				.Afv_Omkostningssted(payment.Afv_Omkostningssted())
				.Afv_PSP_Element(payment.Afv_PSP_Element())
				.Afsendelses_System(payment.Afsendelses_System())
				.Indv_Bemaerkning(payment.Indv_Bemaerkning())
				.Sekvensnummer(Integer.toString(i)) // Set the correct sequence number.
				.reportId(payment.reportId())
				.build();
	}

	@NotNull
	private UpdateEngangsydelser createOnetimePaymentRequest(Report report, double amountToReimburse, LocalDate dateToReport) {
		UpdateEngangsydelser.UpdateEngangsydelserBuilder ydelse = UpdateEngangsydelser.builder();
		ydelse.Startdato(dateToReport.atTime(0,0,0).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
		ydelse.Loenart(String.format("%04d", report.getPayType()));
		ydelse.Loebenummer(report.getSequentialNumber()); // Løbenummer is tied to Lønart
		ydelse.reportId(report.getId());

		double roundedValue = DoubleUtil.round(amountToReimburse);
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat decimalFormat = new DecimalFormat("####0.##", decimalFormatSymbols);
		decimalFormat.setRoundingMode(RoundingMode.DOWN); // Truncate no rounding, already done
		ydelse.Antal(decimalFormat.format(roundedValue)); // "Antal" is refering to the amount of KMs driven under the specified "Loenart" and "Loebenummer"

		// Hvis der er indtastet afvigende omkostningssted eller PSP element af godkenderen.
		if (StringUtils.hasLength(report.getOverrideCostCenter())) {
			ydelse.Afv_Omkostningssted(report.getOverrideCostCenter());
		}
		else if (StringUtils.hasLength(report.getOverridePspElement())) {
			ydelse.Afv_PSP_Element(report.getOverridePspElement());
		}

		ydelse.Afsendelses_System("OS2indberetning");
		ydelse.Indv_Bemaerkning(Long.toString(report.getId()));
		return ydelse.build();
	}

	public void sendReports() {
		List<Report> unprocessed = reportService.getUnprocessed();

		if (unprocessed.isEmpty()) {
			return;
		}

		log.info("Processing " + unprocessed.size() + " unprocessed reports to Opus Onetime Payments API");
		HashMap<String, HashMap<String, List<Report>>> map = organiseReportsByEmployeeNumberAndMonth(unprocessed);

		for (Map.Entry<String, HashMap<String, List<Report>>> entry : map.entrySet()) {
			String employeeNumber = entry.getKey();

			List<UpdateEngangsydelser> updates = processOneMonth(entry, employeeNumber);

			List<Report> allReports = new ArrayList<>();
			entry.getValue().values().forEach(allReports::addAll);
			Map<Long, Report> reportsMap = allReports.stream().collect(Collectors.toMap(Report::getId, Function.identity()));

			List<Report> finishedReports = new ArrayList<>();
			// Send reports in batches
			int batchSize = 999; // Max accepted elements by OPUS (we probably won't hit this, but just in case we handle it)
			int totalSize = updates.size();
			for (int i = 0; i < totalSize; i += batchSize) {
				int end = Math.min(totalSize, i + batchSize);
				List<UpdateEngangsydelser> batch = updates.subList(i, end);
				// Call the API with the current batch
				boolean success = updateOnetimePayments(employeeNumber, batch);
				if (success) {
					for (UpdateEngangsydelser updateEngangsydelser : batch) {
						finishedReports.add(reportsMap.get(updateEngangsydelser.reportId()));
					}
				}
			}

			finishedReports.forEach(report -> {
				report.setProcessedDate(LocalDateTime.now());
				report.setStatus(ReportStatus.REJECTED_AFTER_INVOICE.equals(report.getStatus()) ? ReportStatus.REJECTED : ReportStatus.INVOICED);
			});

			reportService.saveAll(finishedReports);

		}
	}

	private @NotNull List<UpdateEngangsydelser> processOneMonth(Map.Entry<String, HashMap<String, List<Report>>> entry, String employeeNumber) {
		List<UpdateEngangsydelser> updates = new ArrayList<>();

		for (Map.Entry<String, List<Report>> perMonthReports : entry.getValue().entrySet()) {
			LocalDate date = LocalDate.parse(perMonthReports.getKey(), YEAR_AND_MONTH_FORMATTER);
			LocalDate beginningOfMonth = date.withDayOfMonth(1);
			LocalDate endOfMonth = date.withDayOfMonth(date.getMonth().length(date.isLeapYear()));

			LocalDate dateToReport = endOfMonth;
			boolean isFirstReportOfMonth = true;

			// Get entire onetimePayments for the month of the updated report.
			List<Engangsydelser> registeredPayments = readOnetimePayments(employeeNumber, beginningOfMonth.atStartOfDay(), endOfMonth.atTime(23, 59, 59, 999_999_999));
			HashMap<String, List<Engangsydelser>> registeredMap = new HashMap<>();
			if (registeredPayments != null && !registeredPayments.isEmpty()) {

				// Group OPUS onetimePayments by which report they are a part of
				for (Engangsydelser registeredPayment : registeredPayments) {


					// Filter any report not created by us, and that does not have an associated report
					if (!Objects.equals(registeredPayment.Indv_Bemaerkning_1(), "OS2indberetning") ||
						!StringUtils.hasLength(registeredPayment.Indv_Bemaerkning_2())) {
						continue;
					}

					LocalDate registeredStartDate = LocalDate.parse(registeredPayment.Startdato());
					if (dateToReport.isAfter(registeredStartDate)) {
						dateToReport = registeredStartDate; // Keep looking for an earlier date to report
						isFirstReportOfMonth = false;
					}

					if (!registeredMap.containsKey(registeredPayment.Indv_Bemaerkning_2())) {
						registeredMap.put(registeredPayment.Indv_Bemaerkning_2(), new ArrayList<>());
					}

					registeredMap.get(registeredPayment.Indv_Bemaerkning_2()).add(registeredPayment);
				}
			}

			for (Report report : perMonthReports.getValue()) {
				if (registeredMap.containsKey(Long.toString(report.getId()))) {
					// This specific report has already been sent at least once to OPUS, calculate the difference in amounts and create a new request based on that.
					List<Engangsydelser> onetimePayments = registeredMap.get(Long.toString(report.getId()));
					double registeredAmount = DoubleUtil.round(onetimePayments.stream().map(Engangsydelser::Antal).reduce(Double::sum).get());
					double reportedAmount = getDistance(report);
					if (!Objects.equals(reportedAmount, registeredAmount)) {
						double difference = reportedAmount - registeredAmount;
						if (difference != 0) {
							updates.add(createOnetimePaymentRequest(report, difference, dateToReport));
						}
					}
				}
				else {
					// First report sets the date for all coming reports
					if (isFirstReportOfMonth) {
						dateToReport = report.getDriveDate();
						isFirstReportOfMonth = false;
					}

					// This specific report has never been sent to OPUS send the full amount.
					if (getDistance(report) != 0) {
						updates.add(createOnetimePaymentRequest(report, report.getDistance(), dateToReport));
					}
				}
			}
		}
		return updates;
	}

	/**
	 * Fetches distance of a report for the purpose of updating OPUS. Generally it just returns the calculated distance of the report.
	 * But if the report has been rejected after the first invoice, we set the correct value to zero.
	 */
	private static double getDistance(Report report) {
		switch (report.getStatus()) {
			case ACCEPTED -> {
				return report.getDistance();
			}
			case REJECTED_AFTER_INVOICE -> {
				return 0;
			}
			default -> throw new IllegalStateException("Unexpected value: " + report.getStatus());
		}
	}

	private static @NotNull HashMap<String, HashMap<String, List<Report>>> organiseReportsByEmployeeNumberAndMonth(List<Report> unprocessed) {
		HashMap<String, HashMap<String, List<Report>>> map = new HashMap<>();
		for (Report report : unprocessed) {
			if (!map.containsKey(report.getEmployeeNumber())) {
				map.put(report.getEmployeeNumber(), new HashMap<>());
			}

			if (!map.get(report.getEmployeeNumber()).containsKey(report.getDriveDate().format(YEAR_AND_MONTH_FORMATTER))) {
				map.get(report.getEmployeeNumber()).put(report.getDriveDate().format(YEAR_AND_MONTH_FORMATTER), new ArrayList<>());
			}

			map.get(report.getEmployeeNumber()).get(report.getDriveDate().format(YEAR_AND_MONTH_FORMATTER)).add(report);
		}
		return map;
	}
}
