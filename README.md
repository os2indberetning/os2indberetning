# OS2indberetning

Web application for Danish municipalities to manage employee transport-expense reporting (*kørselsgodtgørelse*). Employees report driven routes, approvers approve or reject them, and approved reports are exported to the municipality's payroll system (KMD OPUS) as one-time payments (*engangsydelser*).

- **Stack:** Java 21, Spring Boot 3.5.7, Spring Modulith, Thymeleaf + jQuery/DataTables, Spring Data JPA (Hibernate) with Envers auditing, Flyway, MariaDB/MySQL.
- **Auth:** SAML (MitID / institutional IdP) via the Digital Identity `saml-module`.
- **Runs on:** port **9090**.

---

## What it does

1. **Reporting** — an employee (or the mobile app) creates a report describing a driven route between addresses. The system calculates the distance and the reimbursable amount using configurable per-km rates.
2. **Approval** — a leader or their substitute reviews reports for their organisational unit and approves or rejects them. Rejections notify the employee by email.
3. **Payroll export** — approved reports are grouped per employee per month and pushed to KMD OPUS as one-time payments on a nightly schedule. Business errors returned by OPUS (invalid cost centre, locked employee, invalid PSP element, …) are captured and surfaced for correction.
4. **Supporting rules** — Danish tax rules such as the **60-day rule** are tracked automatically; addresses are geocoded and "washed" against the national address register (DAWA); route distances are computed via an external OSRM routing backend (Septima).

## How it works

### Domain model

`Person` is the hub. A person has one or more `Employment`s, each tied to an `OrgUnit` (organisational units form a self-referential tree). Reports belong to a person + employment and carry a `Route` with GPS coordinates. Approval authority flows from org-unit leadership and from `Substitute` delegations.

```
Person ──< Employment ──> OrgUnit (tree: parent/children)
  │             │
  │             └──< Report ──> Route ──< GpsCoordinate
  ├──< Address
  ├──< PersonalRoute
  └── Substitute (substitute / substituteFor)
```

Reports move through a `ReportStatus` lifecycle (created → pending approval → approved → exported to payroll, or rejected). Rates are defined per `RateType` (`Rate.ratePerKm`).

### Layered architecture

All code lives under `dk.digitalidentity.indberetning`:

| Layer | Package | Responsibility |
|---|---|---|
| MVC controllers | `controller/mvc/` | Thymeleaf pages (report, approve, admin, settings, …) |
| REST controllers | `controller/rest/` | JSON endpoints for browser AJAX / DataTables and the mobile app (`/rest/*`, `/appapi/*`) |
| API controllers | `controller/api/` | External integration endpoints — OPUS/org sync, route data, geocoding (`/api/*`, `/ext/api/*`, `/internal/api/*`, `/routeapi/*`) |
| Services | `service/` | ~50 services holding business logic |
| Entities | `model/entity/` | JPA entities (Hibernate + Envers audit) |
| Repositories | `model/dao/` | Spring Data JPA |
| DTOs | `model/dto/`, `service/dto/` | Request/response and integration payloads |
| Scheduled tasks | `task/` | 16 `@Scheduled` jobs (deadlines, cleanup, OPUS sends, recalculation) |
| Security | `security/`, `filter/` | SAML login post-processing, role annotations, API-key filters |
| Config | `config/` | Spring config; app properties bound under `indberet.*` |

### Key services

- **ReportService** — report lifecycle: create/update/delete, approve, reject, pending-approver updates, rejection emails.
- **OnetimePaymentsCalculatorService** — distance and payment calculation, report recalculation.
- **OnetimePaymentsExportService** — the OPUS payroll export (read existing payments, compute deltas, batch-send).
- **AddressService** / **RouteService** — DAWA geocoding + address washing, and Septima route/distance calculation.
- **SixtyDayRuleService** — Danish 60-day tax-rule tracking and reset.
- **SubstituteService** — delegated-approver rules and change detection.
- **DeadlineEmailsService** — approver deadline reminders and admin address-wash notifications (via the DI email queue).

### External integrations

| System | Purpose | Entry point |
|---|---|---|
| **KMD OPUS** | Payroll — one-time payments export (mTLS client cert) | `OnetimePaymentsExportService`, `OrganisationAPIController` |
| **Septima** | OSRM car routing / distance | `RouteService` |
| **DAWA / Dataforsyningen** | Address lookup, washing, reverse geocoding | `AddressService`, `RouteService` |
| **DI email service** | Transactional email (DB-backed queue) | `DeadlineEmailsService`, `ReportService` |
| **SAML IdP** | Login (MitID / institutional) | `saml-module`, `security/LoginPostProcessor` |

Every integration is designed to fail soft: failures are logged at `WARN` and do not abort the
surrounding operation. Business errors returned by OPUS are persisted and surfaced for correction
rather than discarded.

---

## Build and run

```bash
./mvnw clean package    # build the jar (indberetning-0.0.1-SNAPSHOT.jar)
./mvnw test             # run tests (requires Docker — MariaDB Testcontainer)
./mvnw spotless:check   # verify formatting (gates CI)
./mvnw spotless:apply   # auto-format
```

Tests use a **MariaDB 10.11 Testcontainer**, so Docker must be running. In CI set `TESTCONTAINERS_RYUK_DISABLED=true`.

### Configuration

Properties load from `src/main/resources/default.properties` (baseline, always) and are overridden by the external `config/application.properties`. Local dev overrides go in `config/application.development.properties` (gitignored). All application settings are bound under the `indberet.*` prefix (see `config/settings/OS2indberetningConfiguration.java` and siblings).

Flyway runs automatically on startup (migrations in `src/main/resources/db/migration/`, plus the repeatable reporting view `R__view_reports.sql`).

### Docker

Multi-stage build on `amazoncorretto:21`; runtime exposes port 9090, entrypoint `deploy/run.sh` (JVM `-Xmx256m`, timezone `Europe/Copenhagen`). CI runs Spotless formatting checks plus a Java 21 build and test on every change.

---

## Conventions

Project-specific conventions apply to dependency injection, security annotations, Thymeleaf fragments and SQL migrations. Notable rules:

- Every controller carries exactly one class-level security annotation (`@RequireAdministrator`, `@RequireApprover`, …); enforced by a reflection-based test.
- Controller request/response types are inline `record`s, one set per method.
- New Flyway migrations must be exactly one higher than the current highest; views go in the repeatable migration.
- Run `./mvnw spotless:apply` before committing.
