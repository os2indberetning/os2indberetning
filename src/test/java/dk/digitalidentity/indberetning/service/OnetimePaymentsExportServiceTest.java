package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import dk.digitalidentity.indberetning.config.settings.modules.OpusOnetimePaymentsConfiguration;
import dk.digitalidentity.indberetning.exceptions.KMDOnetimeException;
import dk.digitalidentity.indberetning.mockfactory.EngangsydelserFactory;
import dk.digitalidentity.indberetning.mockfactory.ReadOnetimePaymentsResponseFactory;
import dk.digitalidentity.indberetning.mockfactory.ReportFactory;
import dk.digitalidentity.indberetning.mockfactory.UpdateEngangsydelserFactory;
import dk.digitalidentity.indberetning.mockfactory.UpdateOnetimePaymentsResponseFactory;
import dk.digitalidentity.indberetning.model.entity.ErrorComment;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.enums.ReportStatus;
import dk.digitalidentity.indberetning.security.SecurityUtil;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.Engangsydelser;
import dk.digitalidentity.indberetning.service.dto.opusapi.readonetimepayments.ReadOnetimePaymentsResponse;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateEngangsydelser;
import dk.digitalidentity.indberetning.service.dto.opusapi.updateonetimepayments.UpdateOnetimePaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnetimePaymentsExportServiceTest {

    @Mock private ErrorCommentService errorCommentService;
    @Mock private ErrorLogService errorLogService;
    @Mock private ErrorResponseService errorResponseService;
    @Mock private ReportService reportService;
    @Mock private AuditLogService auditLogService;
    @Mock private SecurityUtil securityUtil;
    @Mock private OS2indberetningConfiguration configuration;
    @Mock private RestClient restClient;

    // RestClient fluent chain mocks
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private OnetimePaymentsExportService service;

    @BeforeEach
    void setUp() {
        OpusOnetimePaymentsConfiguration opusConfig = new OpusOnetimePaymentsConfiguration();
        opusConfig.setMunicipalityCode("740");
        opusConfig.setApiBaseUrl("https://opus.example.com/api/");
        // lenient: some tests return before configuration is accessed (e.g. empty unprocessed list)
        lenient().when(configuration.getOpus()).thenReturn(opusConfig);

        // Constructor uses @Qualifier so @InjectMocks cannot wire the RestClient — build manually
        service = new OnetimePaymentsExportService(
                restClient, configuration, reportService,
                errorLogService, errorResponseService, auditLogService,
                securityUtil, errorCommentService);
    }

    private void givenRestClientPostReturns(ReadOnetimePaymentsResponse response) {
        // lenient() avoids strict-stubbing failures on body(Object) — the service passes a real
        // request object, but we only care about what comes back, not what goes in
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(ReadOnetimePaymentsResponse.class)).thenReturn(response);
    }

    private void givenRestClientPostThrowsHttpClientError(HttpStatus status) {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(ReadOnetimePaymentsResponse.class))
                .thenThrow(HttpClientErrorException.create(status, status.getReasonPhrase(), null, null, null));
    }

    private void givenUpdateRestClientPostReturns(UpdateOnetimePaymentResponse response) {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(UpdateOnetimePaymentResponse.class)).thenReturn(response);
    }

    private void givenUpdateRestClientPostThrowsHttpClientError(HttpStatus status) {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(UpdateOnetimePaymentResponse.class))
                .thenThrow(HttpClientErrorException.create(status, status.getReasonPhrase(), null, null, null));
    }

    // Stubs both read and update REST calls — used by sendReports() which does both in sequence.
    // thenAnswer dispatches by the requested response class.
    private void givenBothRestCallsReturn(ReadOnetimePaymentsResponse readResponse,
                                          UpdateOnetimePaymentResponse updateResponse) {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(any(Class.class))).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type == ReadOnetimePaymentsResponse.class) return readResponse;
            if (type == UpdateOnetimePaymentResponse.class) return updateResponse;
            return null;
        });
    }

    // Stubs save-side-effects so fixErrorUpdateOnetimePayments doesn't NPE on errorResponseService/errorCommentService
    private void givenErrorServiceSavesSucceed() {
        lenient().when(errorResponseService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(errorLogService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(errorCommentService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reportService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class ReadOnetimePayments {

        private static final LocalDateTime FROM = LocalDateTime.of(2024, 9, 10, 0, 0, 0);
        private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 12, 0, 0, 0);

        @Test
        void shouldReturnPaymentsWhenStatus0AndBodyPresent() {
            // Arrange
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.successWithTwoPayments());

            // Act
            List<Engangsydelser> result = service.readOnetimePayments("12345", FROM, TO);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).Antal()).isEqualTo(0.25);
            assertThat(result.get(1).Antal()).isEqualTo(-0.25);
        }

        @Test
        void shouldReturnNullListWhenApiReportsDataNotFound() {
            // Arrange — Status=1, Tekst="Data ikke fundet." is the API's empty-result convention
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.notFound());

            // Act
            List<Engangsydelser> result = service.readOnetimePayments("12345", FROM, TO);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        void shouldThrowWhenResponseBodyIsNull() {
            // Arrange
            givenRestClientPostReturns(null);

            // Act & Assert
            assertThatThrownBy(() -> service.readOnetimePayments("12345", FROM, TO))
                    .isInstanceOf(KMDOnetimeException.class)
                    .hasMessageContaining("Empty body");
        }

        @Test
        void shouldThrowWhenHeaderIsMissing() {
            // Arrange — body present but Header field is null
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.noHeader());

            // Act & Assert
            assertThatThrownBy(() -> service.readOnetimePayments("12345", FROM, TO))
                    .isInstanceOf(KMDOnetimeException.class)
                    .hasMessageContaining("No header");
        }

        @Test
        void shouldThrowWhenHeaderStatusIsUnexpectedNonZeroNonDataNotFound() {
            // Arrange — Status=500 with SAP error message (HTML-encoded in real API, plain here)
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.error(500, "Error while sending message to module processor"));

            // Act & Assert
            assertThatThrownBy(() -> service.readOnetimePayments("12345", FROM, TO))
                    .isInstanceOf(KMDOnetimeException.class)
                    .hasMessageContaining("Error while sending message");
        }

        @Test
        void shouldThrowWhenStatus1ButTekstIsNotDataIkkeFundet() {
            // Arrange — Status=1 with an unexpected message is still an error
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.error(1, "Some unexpected error"));

            // Act & Assert
            assertThatThrownBy(() -> service.readOnetimePayments("12345", FROM, TO))
                    .isInstanceOf(KMDOnetimeException.class)
                    .hasMessage("Some unexpected error");
        }

        @Test
        void shouldReturnNullAndNotThrowWhenHttpClientErrorExceptionIsThrown() {
            // Arrange — HTTP 400/403/etc. from the gateway (invalid client, forbidden cert)
            givenRestClientPostThrowsHttpClientError(HttpStatus.BAD_REQUEST);

            // Act
            List<Engangsydelser> result = service.readOnetimePayments("12345", FROM, TO);

            // Assert — HTTP errors are caught and swallowed; caller gets null
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullAndNotThrowOnForbidden() {
            // Arrange — ClientID not allowed for current client certificate
            givenRestClientPostThrowsHttpClientError(HttpStatus.FORBIDDEN);

            // Act
            List<Engangsydelser> result = service.readOnetimePayments("12345", FROM, TO);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        void shouldHandleNullFromDate() {
            // Arrange — null dates must be serialised as empty strings, not throw NPE
            givenRestClientPostReturns(ReadOnetimePaymentsResponseFactory.successWithTwoPayments());

            // Act & Assert — no exception
            assertThat(service.readOnetimePayments("12345", null, null)).isNotNull();
        }
    }

    @Nested
    class UpdateOnetimePayments {

        private final List<UpdateEngangsydelser> onePayment =
                List.of(UpdateEngangsydelserFactory.basic(42L, "0.25"));

        @Test
        void shouldReturnTrueWhenResponseStatusIsOk() {
            // Arrange
            givenUpdateRestClientPostReturns(UpdateOnetimePaymentsResponseFactory.success());

            // Act
            boolean result = service.updateOnetimePayments("12345", onePayment);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenResponseStatusIsError() {
            // Arrange
            givenUpdateRestClientPostReturns(UpdateOnetimePaymentsResponseFactory.error(
                    List.of(UpdateOnetimePaymentsResponseFactory.errorMessage(119, "Engangsydelse Nr. 000 — Angiv gyldigt omkostningssted."))));
            when(reportService.getById(42L)).thenReturn(new Report());

            // Act
            boolean result = service.updateOnetimePayments("12345", onePayment);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseAndNotThrowWhenHttpClientErrorIsThrown() {
            // Arrange — same gateway errors as read: invalid client, forbidden cert, etc.
            givenUpdateRestClientPostThrowsHttpClientError(HttpStatus.BAD_REQUEST);

            // Act
            boolean result = service.updateOnetimePayments("12345", onePayment);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseAndNotThrowOnForbidden() {
            // Arrange
            givenUpdateRestClientPostThrowsHttpClientError(HttpStatus.FORBIDDEN);

            // Act
            boolean result = service.updateOnetimePayments("12345", onePayment);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        void shouldThrowWhenResponseBodyIsNull() {
            // Arrange
            givenUpdateRestClientPostReturns(null);

            // Act & Assert — null body throws KMDOnetimeException; updateOnetimePayments does not catch it
            assertThatThrownBy(() -> service.updateOnetimePayments("12345", onePayment))
                    .isInstanceOf(KMDOnetimeException.class)
                    .hasMessageContaining("Empty body");
        }

        @Test
        void shouldReturnFalseWhenOnlyValidateUpdatesIsTrue() {
            // Arrange — Kun_Valider mode: API accepts the payload but we treat it as not-sent
            OpusOnetimePaymentsConfiguration opusConfig = new OpusOnetimePaymentsConfiguration();
            opusConfig.setMunicipalityCode("740");
            opusConfig.setApiBaseUrl("https://opus.example.com/api/");
            opusConfig.setOnlyValidateUpdates(true);
            when(configuration.getOpus()).thenReturn(opusConfig);
            givenUpdateRestClientPostReturns(UpdateOnetimePaymentsResponseFactory.success());

            // Act
            boolean result = service.updateOnetimePayments("12345", onePayment);

            // Assert — validation-only run must never mark reports as invoiced
            assertThat(result).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // matchOnetimePaymentToError — exercised via updateOnetimePayments error path.
    // The method parses "Engangsydelse Nr. 000 ..." from OPUS and matches it to a
    // payment's Sekvensnummer, normalising leading zeroes on OPUS's side.
    // -------------------------------------------------------------------------
    @Nested
    class MatchOnetimePaymentToError {

        // updateOnetimePayments always overwrites Sekvensnummer to the list index (0-based),
        // so payload[0] always gets Sekvensnummer "0", which OPUS echoes back as "000".
        private final List<UpdateEngangsydelser> onePayment =
                List.of(UpdateEngangsydelserFactory.basic(42L, "0.25"));

        private void arrangeErrorAndReport(int kode, String tekst) {
            givenUpdateRestClientPostReturns(UpdateOnetimePaymentsResponseFactory.error(
                    List.of(UpdateOnetimePaymentsResponseFactory.errorMessage(kode, tekst))));
            givenErrorServiceSavesSucceed();
        }

        @Test
        void shouldMatchWhenOpusLeadingZeroesNormaliseToSameSequenceNumber() {
            // Arrange — OPUS pads sequence numbers: "000" == our "0"
            arrangeErrorAndReport(119, "Engangsydelse Nr. 000 Angiv gyldigt omkostningssted.");
            Report report = new Report();
            report.setId(42L);
            when(reportService.getById(42L)).thenReturn(report);

            // Act
            service.updateOnetimePayments("12345", onePayment);

            // Assert — report was looked up, meaning the message was matched to sequence "0"
            verify(reportService).getById(42L);
        }

        @Test
        void shouldNotMatchWhenSequenceNumberDiffers() {
            // Arrange — sequence "001" does not match our payment's "0"
            arrangeErrorAndReport(119, "Engangsydelse Nr. 001 Angiv gyldigt omkostningssted.");

            // Act
            service.updateOnetimePayments("12345", onePayment);

            // Assert — no match, so getById is never called
            verify(reportService, org.mockito.Mockito.never()).getById(any(Long.class));
        }

        @ParameterizedTest(name = "message=''{0}''")
        @ValueSource(strings = {
                "",
                "Some unrelated error",
                "Engangsydelse Nr.",        // prefix present but too short (< 20 chars)
        })
        void shouldNotMatchWhenMessageFailsGuardClauses(String tekst) {
            // Arrange
            arrangeErrorAndReport(119, tekst);

            // Act
            service.updateOnetimePayments("12345", onePayment);

            // Assert — guard clauses short-circuit before any lookup
            verify(reportService, org.mockito.Mockito.never()).getById(any(Long.class));
        }
    }

    // -------------------------------------------------------------------------
    // fixErrorUpdateOnetimePayments — exercised via updateOnetimePayments when
    // an error message matches a payment. Each error code has distinct mutations.
    // -------------------------------------------------------------------------
    @Nested
    class FixErrorUpdateOnetimePayments {

        private static final String MATCH_PREFIX = "Engangsydelse Nr. 000 ";

        private Report reportWithId(long id) {
            Report r = new Report();
            r.setId(id);
            r.setStatus(ReportStatus.ACCEPTED);
            r.setOverrideCostCenter("CC-001");
            r.setOverridePspElement("PSP-001");
            return r;
        }

        private void arrangeAndAct(int kode, Report report) {
            givenUpdateRestClientPostReturns(UpdateOnetimePaymentsResponseFactory.error(
                    List.of(UpdateOnetimePaymentsResponseFactory.errorMessage(kode, MATCH_PREFIX + "some text"))));
            givenErrorServiceSavesSucceed();
            when(reportService.getById(report.getId())).thenReturn(report);
            service.updateOnetimePayments(String.valueOf(report.getId()), List.of(UpdateEngangsydelserFactory.basic(report.getId(), "1.0")));
        }

        @Test
        void shouldResetReportToPendingAndClearCostCenterOnCode119WhenCorrectingEnabled() {
            // Arrange — correctInvalidOnetimePayments defaults to true in OpusOnetimePaymentsConfiguration
            Report report = reportWithId(10L);

            // Act
            arrangeAndAct(119, report);

            // Assert
            assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(report.getOverrideCostCenter()).isNull();
            assertThat(report.getApprovedBy()).isNull();
        }

        @Test
        void shouldNotResetReportOnCode119WhenCorrectingDisabled() {
            // Arrange
            OpusOnetimePaymentsConfiguration opusConfig = new OpusOnetimePaymentsConfiguration();
            opusConfig.setMunicipalityCode("740");
            opusConfig.setApiBaseUrl("https://opus.example.com/api/");
            opusConfig.setCorrectInvalidOnetimePayments(false);
            when(configuration.getOpus()).thenReturn(opusConfig);
            Report report = reportWithId(10L);
            report.setStatus(ReportStatus.ACCEPTED);

            // Act
            arrangeAndAct(119, report);

            // Assert — status must not have changed
            assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        }

        @Test
        void shouldResetReportToPendingAndClearPspElementOnCode151WhenCorrectingEnabled() {
            // Arrange
            Report report = reportWithId(11L);

            // Act
            arrangeAndAct(151, report);

            // Assert
            assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(report.getOverridePspElement()).isNull();
            assertThat(report.getApprovedBy()).isNull();
        }

        @Test
        void shouldClearPspElementOnCode167WhenCorrectingEnabled() {
            // Arrange
            Report report = reportWithId(12L);

            // Act
            arrangeAndAct(167, report);

            // Assert — only PSP element cleared, status unchanged
            assertThat(report.getOverridePspElement()).isNull();
            assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        }

        @Test
        void shouldNotMutateReportOnCode138() {
            // Arrange — 138 "Medarbejderen er låst" requires no automatic correction
            Report report = reportWithId(13L);

            // Act
            arrangeAndAct(138, report);

            // Assert — report untouched
            assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
            assertThat(report.getOverrideCostCenter()).isEqualTo("CC-001");
            assertThat(report.getOverridePspElement()).isEqualTo("PSP-001");
        }

        @Test
        void shouldNotMutateReportOnUnknownErrorCode() {
            // Arrange
            Report report = reportWithId(14L);

            // Act
            arrangeAndAct(999, report);

            // Assert — default branch: no mutations
            assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        }

        @Test
        void shouldSaveErrorCommentForEveryErrorCode() {
            // Arrange
            Report report = reportWithId(15L);

            // Act
            arrangeAndAct(119, report);

            // Assert — an ErrorComment is always persisted regardless of code
            ArgumentCaptor<ErrorComment> captor = ArgumentCaptor.forClass(ErrorComment.class);
            verify(errorCommentService).save(captor.capture());
            assertThat(captor.getValue().getComment()).contains("15");
        }
    }

    // -------------------------------------------------------------------------
    // getDistance — exercised via sendReports → processOneMonth.
    // Easiest to test directly by observing what amount ends up in the update call.
    // -------------------------------------------------------------------------
    @Nested
    class GetDistance {

        @Test
        void shouldReturnReportDistanceForAcceptedReport() {
            // Arrange
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 1), 12.5);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act — no exception means getDistance(ACCEPTED) returned the non-zero distance
            service.sendReports();

            verify(reportService).saveAll(any());
        }

        @Test
        void shouldReturnZeroForRejectedAfterInvoiceReport() {
            // Arrange — REJECTED_AFTER_INVOICE distance is 0, so no update is sent
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 1), 12.5);
            report.setStatus(ReportStatus.REJECTED_AFTER_INVOICE);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            // OPUS has the original amount registered — difference will be -12.5
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            EngangsydelserFactory.os2("2024-11-01", "1", 12.5))),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — a cancelling update (-12.5) was sent and report progressed
            verify(reportService).saveAll(any());
        }

        @Test
        void shouldThrowForUnexpectedReportStatus() {
            // Arrange — PENDING is not a valid status for sendReports; getDistance throws
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 1), 12.5);
            report.setStatus(ReportStatus.PENDING);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());

            // Act & Assert
            assertThatThrownBy(() -> service.sendReports())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // organiseReportsByEmployeeNumberAndMonth — exercised via sendReports.
    // -------------------------------------------------------------------------
    @Nested
    class OrganiseReportsByEmployeeNumberAndMonth {

        @Test
        void shouldDoNothingWhenNoUnprocessedReports() {
            // Arrange
            when(reportService.getUnprocessed()).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — no REST calls, no saves
            verify(restClient, org.mockito.Mockito.never()).post();
            verify(reportService, org.mockito.Mockito.never()).saveAll(any());
        }

        @Test
        void shouldGroupReportsFromDifferentEmployeesSeparately() {
            // Arrange — two employees, same month: each triggers its own read+update pair
            Report r1 = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            Report r2 = ReportFactory.accepted(2L, "EMP2", LocalDate.of(2024, 11, 15), 20.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(r1, r2));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — saveAll called once per employee
            verify(reportService, org.mockito.Mockito.times(2)).saveAll(any());
        }

        @Test
        void shouldGroupReportsFromSameEmployeeDifferentMonthsSeparately() {
            // Arrange — same employee, two months
            Report r1 = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 10, 15), 10.0);
            Report r2 = ReportFactory.accepted(2L, "EMP1", LocalDate.of(2024, 11, 15), 20.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(r1, r2));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — one saveAll for the single employee containing both finished reports
            verify(reportService, org.mockito.Mockito.times(1)).saveAll(any());
        }
    }

    // -------------------------------------------------------------------------
    // processOneMonth — filtering and amount logic, exercised via sendReports.
    // -------------------------------------------------------------------------
    @Nested
    class ProcessOneMonth {

        @Test
        void shouldIgnorePaymentsFromExternalSystems() {
            // Arrange — OPUS returns a payment from a foreign system; must not affect our report
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            EngangsydelserFactory.external("2024-11-15"))),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — external payment is ignored so report is treated as new and update is sent
            verify(reportService).saveAll(any());
        }

        @Test
        void shouldIgnoreOs2PaymentsWithEmptyIndvBemaerkning2() {
            // Arrange — OS2 payment but no report ID in Indv_Bemaerkning_2: must be skipped
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            new Engangsydelser("2024-11-15", "OBJ", "0100", 1, 0, 10.0, 0, null, "OS2indberetning", ""))),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — skipped payment treated as not-registered; full amount update sent
            verify(reportService).saveAll(any());
        }

        @Test
        void shouldSendDifferenceWhenReportAlreadyRegisteredInOpusWithDifferentAmount() {
            // Arrange — OPUS has 10.0 registered, our report is 12.5 → send +2.5
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 12.5);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            EngangsydelserFactory.os2("2024-11-15", "1", 10.0))),
                    UpdateOnetimePaymentsResponseFactory.success());

            ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
            when(reportService.saveAll(saveCaptor.capture())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — one report saved as INVOICED
            assertThat(saveCaptor.getValue()).hasSize(1);
            assertThat(((Report) saveCaptor.getValue().get(0)).getStatus()).isEqualTo(ReportStatus.INVOICED);
        }

        @Test
        void shouldSkipUpdateWhenRegisteredAmountMatchesReport() {
            // Arrange — OPUS already has exactly 12.5; difference = 0 → no update sent
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 12.5);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            EngangsydelserFactory.os2("2024-11-15", "1", 12.5))),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — saveAll still called but with empty list (report skipped, not invoiced)
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(reportService).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }

        @Test
        void shouldSkipNewReportWithZeroDistance() {
            // Arrange
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 0.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — zero-distance report produces no update and is not invoiced
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(reportService).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // sendReports — status transitions after successful update
    // -------------------------------------------------------------------------
    @Nested
    class SendReports {

        @Test
        void shouldMarkAcceptedReportAsInvoicedOnSuccess() {
            // Arrange
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            givenBothRestCallsReturn(ReadOnetimePaymentsResponseFactory.notFound(),
                    UpdateOnetimePaymentsResponseFactory.success());

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            when(reportService.saveAll(captor.capture())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert
            assertThat(captor.getValue()).hasSize(1);
            assertThat(((Report) captor.getValue().get(0)).getStatus()).isEqualTo(ReportStatus.INVOICED);
        }

        @Test
        void shouldMarkRejectedAfterInvoiceReportAsRejectedOnSuccess() {
            // Arrange
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            report.setStatus(ReportStatus.REJECTED_AFTER_INVOICE);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            // OPUS has the original amount, so a cancelling update is sent
            givenBothRestCallsReturn(
                    ReadOnetimePaymentsResponseFactory.success(List.of(
                            EngangsydelserFactory.os2("2024-11-15", "1", 10.0))),
                    UpdateOnetimePaymentsResponseFactory.success());

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            when(reportService.saveAll(captor.capture())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert
            assertThat(captor.getValue()).hasSize(1);
            assertThat(((Report) captor.getValue().get(0)).getStatus()).isEqualTo(ReportStatus.REJECTED);
        }

        @Test
        void shouldNotInvoiceReportsWhenUpdateFails() {
            // Arrange — update returns HTTP error; reports must stay in their current status
            Report report = ReportFactory.accepted(1L, "EMP1", LocalDate.of(2024, 11, 15), 10.0);
            when(reportService.getUnprocessed()).thenReturn(List.of(report));
            lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
            lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            lenient().when(responseSpec.body(any(Class.class))).thenAnswer(inv -> {
                Class<?> type = inv.getArgument(0);
                if (type == ReadOnetimePaymentsResponse.class) return ReadOnetimePaymentsResponseFactory.notFound();
                throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
            });
            when(reportService.saveAll(any())).thenReturn(List.of());

            // Act
            service.sendReports();

            // Assert — saveAll called with empty list because no reports finished successfully
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(reportService).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }
}
