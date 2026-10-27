# Insurance Policy and Claims Management System

A Spring Boot backend for managing insurance customers, policies, premiums,
and claims — built as a personal portfolio project by Ramesh Poluri.

**Honesty note:** the insurer, the customers, and all data in this project
are fictional demo content created for this project. It makes no claim about
any employer, and nothing here runs on a real cloud account or production
system.

## What it does

- **Customer profiles** — create and look up customers (name, email, phone,
  date of birth, address). Email addresses are unique.
- **Policy creation and management** — create policies for a customer with a
  type (HEALTH, AUTO, HOME, LIFE, TRAVEL), coverage amount, effective date,
  and renewal date. Each policy gets a generated policy number and an
  ACTIVE status.
- **Policy status updates** — PENDING → ACTIVE → EXPIRED / CANCELLED /
  RENEWED, with illegal transitions rejected (for example, an ACTIVE policy
  cannot go back to PENDING).
- **Validation** — customers must be 18 or older; the renewal date must be
  after the effective date; coverage must be positive.
- **Premium calculation** — a transparent formula (see below), available as
  a no-commit quote endpoint and applied automatically when a policy is
  created.
- **Claims** — register a claim against an ACTIVE policy, then move it
  through REGISTERED → UNDER_REVIEW → APPROVED → SETTLED, or REJECTED from
  REGISTERED / UNDER_REVIEW. Claims must fall inside the policy period and
  cannot exceed the coverage still unused on the policy.
- **Persistence** — Hibernate / Spring Data JPA on an H2 database.
- **Events** — every policy/claim creation and status change publishes a
  domain event through a publisher interface (local by default; see below).
- **Security** — HTTP Basic auth protects every `/api/**` endpoint with a
  local demo user (see below). No OAuth provider, no real credentials.
- **Tests** — JUnit 5 + Mockito unit/service tests and MockMvc integration
  tests covering the full customer → policy → claim → settle flow.

## Premium formula

```
premium = coverageAmount x baseRate(policyType) x ageFactor(age)
```

| Policy type | Base annual rate |
| --- | --- |
| HEALTH | 5% |
| AUTO | 4% |
| HOME | 3% |
| LIFE | 2.5% |
| TRAVEL | 6% |

Age factor: under 25 → 1.10, 60 and over → 1.20, otherwise 1.00.
Result is rounded to 2 decimals. Example: a 36-year-old with a HEALTH policy
covering 100,000 pays 100,000 × 0.05 × 1.00 = **5,000**.

## Claim lifecycle

```
REGISTERED → UNDER_REVIEW → APPROVED → SETTLED
     │              │
     └────→ REJECTED (terminal)
```

Settling an APPROVED claim pays `min(claimAmount, remainingCoverage)`,
where remaining coverage = coverage − amounts of other APPROVED/SETTLED
claims on the policy. REJECTED and SETTLED are terminal: no further
transitions are allowed.

## API list

Base URL: `http://localhost:8080` (all endpoints require HTTP Basic auth).

| Method | Path | What it does |
| --- | --- | --- |
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers` | List customers |
| GET | `/api/customers/{id}` | Get one customer |
| POST | `/api/policies` | Create a policy (premium calculated automatically) |
| POST | `/api/policies/quote` | Premium quote without creating a policy |
| GET | `/api/policies` | List policies |
| GET | `/api/policies/{id}` | Get one policy |
| GET | `/api/policies/customer/{customerId}` | Policies for a customer |
| PUT | `/api/policies/{id}/status` | Update policy status |
| POST | `/api/claims` | Register a claim |
| GET | `/api/claims` | List claims |
| GET | `/api/claims/{id}` | Get one claim |
| GET | `/api/claims/policy/{policyId}` | Claims for a policy |
| PUT | `/api/claims/{id}/status` | Move a claim to a new status |
| POST | `/api/claims/{id}/settle` | Settle an APPROVED claim |

Errors use one clean body: `{timestamp, status, error, message, path}`.
A Postman collection covering every endpoint is in
`postman/insurance-claims.postman_collection.json`.

## Security (demo only)

HTTP Basic auth, single in-memory user from `application.properties`:

- Username: `demo`
- Password: `demo123`

Change them in `application.properties` for your own runs. This is a demo
setup, not production security: there is no OAuth provider and no per-user
authorization. Unauthenticated requests get `401`.

## Events and messaging

Every policy creation, policy status change, claim registration, claim
status change, and claim settlement publishes a `DomainEvent` through the
`EventPublisher` interface:

| Setup | What actually runs |
| --- | --- |
| Default (`messaging.enabled=false`) | `LocalEventPublisher`: events are logged and kept in memory. No broker, no cloud, no credentials. |
| External (`messaging.enabled=true` + `messaging.endpoint=<url>`) | `HttpEventPublisher`: events are POSTed as JSON to the configured endpoint (a webhook or messaging bridge). Delivery failures are logged, never fatal to the business operation. |

Nothing in this project talks to a real cloud messaging service unless you
configure an endpoint yourself.

## Run it

```bash
cd insurance-claims-service   # (project root)
mvn spring-boot:run
```

In VS Code: open the project folder, run the
`InsuranceClaimsApplication` class with the Java extension's Run button.

Try it (replace `<id>` values from earlier responses):

```bash
# Create a customer (Basic auth demo/demo123)
curl -u demo:demo123 -X POST localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Priya","lastName":"Nair","email":"priya@example.com",
       "dateOfBirth":"1990-05-15"}'

# Create a policy (premium is calculated: HEALTH 100000 -> 5000.00)
curl -u demo:demo123 -X POST localhost:8080/api/policies \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"policyType":"HEALTH","coverageAmount":100000,
       "effectiveDate":"2026-01-01","renewalDate":"2027-01-01"}'

# Register a claim, then move it through review -> approval -> settlement
curl -u demo:demo123 -X POST localhost:8080/api/claims \
  -H "Content-Type: application/json" \
  -d '{"policyId":1,"description":"Hospital bill","claimAmount":5000,
       "incidentDate":"2026-03-10"}'
curl -u demo:demo123 -X PUT localhost:8080/api/claims/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"UNDER_REVIEW","reason":"Checking documents"}'
curl -u demo:demo123 -X PUT localhost:8080/api/claims/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"APPROVED","reason":"Documents verified"}'
curl -u demo:demo123 -X POST localhost:8080/api/claims/1/settle
```

## Docker

```bash
mvn -o package -DskipTests   # build the jar first
docker compose up --build
```

## Tests

```bash
mvn test
```

## Project layout

```
src/main/java/com/insurance/claims/
  controller/   REST controllers (customers, policies, claims)
  service/      CustomerService, PolicyService, ClaimService, PremiumCalculator
  model/        JPA entities, enums, request DTOs
  repository/   Spring Data JPA repositories
  events/       DomainEvent, EventPublisher interface, local + HTTP publishers
  exception/    Custom exceptions + centralized @RestControllerAdvice handler
  security/     HTTP Basic auth configuration (demo user)
docs/           Architecture, policy/claim lifecycle, event flow diagrams
postman/        Postman collection for all endpoints
.github/        CI workflow (Maven tests)
```

## Diagrams

- `docs/architecture.png` — layers and components
- `docs/policy-claim-lifecycle.png` — policy and claim state machines
- `docs/event-flow.png` — how domain events flow to publishers
