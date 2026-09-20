# LegacyPay

LegacyPay is a payment modernization learning project. It shows how a legacy/mainframe-style payment engine can be wrapped by a modern Java and Spring Boot API, then gradually improved with persistence, transactions, idempotency, auditability, security, observability, events, Kafka integration, and Docker packaging.

The project is intentionally small, but the concepts are real payment-system concepts.

## Final Architecture

```text
Client
  |
  | HTTP Basic + Idempotency-Key
  v
Spring Boot REST API
  |
  v
PaymentController
  |
  v
PaymentService
  |
  | per-key idempotency lock
  v
PaymentProcessor (@Transactional)
  |
  |-- AccountRepository -> JPA/Hibernate -> PostgreSQL
  |-- PaymentTransactionRepository -> JPA/Hibernate -> PostgreSQL
  |-- AuditService -> audit_logs
  |-- PaymentMetrics -> Micrometer/Actuator
  |-- PaymentEventPublisher -> Spring domain event
                              |
                              v
                         Kafka producer
                         (enabled by env flag)

LegacyPaymentAdapter -> LegacyPaymentEngine
```

The legacy adapter remains isolated. In a real modernization program, the internals behind `LegacyPaymentAdapter` could be replaced later without changing the public REST API.

## Completed Features

- Spring Boot REST API
- `GET /api/health`
- `POST /api/payments`
- DTO validation and global API errors
- Simulated legacy payment engine with mainframe-style result codes
- JPA/Hibernate account persistence
- Flyway-managed schema migrations
- PostgreSQL runtime configuration
- H2 test database
- Transactional payment processing
- Account debit and credit using `BigDecimal`
- Payment transaction history
- Idempotency-Key support
- Concurrency-safe duplicate request handling on a single app node
- Payment lifecycle: `INITIATED`, `PROCESSING`, `COMPLETED`, `FAILED`
- Payment status lookup
- Audit log storage and lookup
- HTTP Basic authentication for payment and audit endpoints
- Public health endpoint
- Spring Boot Actuator health and metrics
- Payment outcome metrics
- In-process domain events
- Optional Kafka publishing/consuming
- Kafka retry and dead-letter configuration
- Dockerfile and Compose setup for app, PostgreSQL, and Kafka

## Database

Flyway migrations live in `src/main/resources/db/migration`:

- `V1__create_accounts.sql`
- `V2__create_payment_transactions.sql`
- `V3__seed_sample_accounts.sql`
- `V4__create_audit_logs.sql`

Sample accounts:

| Account | Starting Balance |
|---|---:|
| `A100` | `5000.00` |
| `A200` | `1000.00` |

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=validate`, so Flyway owns schema creation and changes.

## API Examples

Development credentials are configurable. The defaults are:

- username: `dev-client`
- password: `dev-password`

For real use, override them with environment variables.

### Health

```bash
curl http://localhost:8080/api/health
```

### Create Payment

```bash
curl -i -u dev-client:dev-password \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-payment-001" \
  -d '{"senderAccount":"A100","receiverAccount":"A200","amount":500}' \
  http://localhost:8080/api/payments
```

Successful business response:

```json
{
  "status": "ACCEPTED",
  "message": "Payment request received"
}
```

Insufficient funds business response:

```json
{
  "status": "REJECTED",
  "message": "INSUFFICIENT_FUNDS"
}
```

Missing or invalid request data returns an HTTP error with:

```json
{
  "status": 400,
  "message": "..."
}
```

Reusing the same `Idempotency-Key` with the same payment details returns the original result without moving money again. Reusing the same key with different payment details returns HTTP `409`.

### Payment Status

```bash
curl -u dev-client:dev-password http://localhost:8080/api/payments/1
```

### Audit Logs

```bash
curl -u dev-client:dev-password http://localhost:8080/api/audit/payments/1
```

### Actuator

```bash
curl http://localhost:8080/actuator/health
curl -u dev-client:dev-password http://localhost:8080/actuator/metrics/legacypay.payments
```

## Local Run

You need Java 21, Maven, and PostgreSQL.

```bash
export LEGACYPAY_DB_URL=jdbc:postgresql://localhost:5432/legacypay
export LEGACYPAY_DB_USERNAME=legacypay
export LEGACYPAY_DB_PASSWORD=your-db-password
export LEGACYPAY_APP_USERNAME=dev-client
export LEGACYPAY_APP_PASSWORD=dev-password

mvn spring-boot:run
```

Kafka is disabled by default. To enable Kafka event publishing:

```bash
export LEGACYPAY_KAFKA_ENABLED=true
export LEGACYPAY_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Docker Run

Copy the example environment file, edit values if needed, then start Compose:

```bash
cp .env.example .env
docker compose up --build
```

Services:

- LegacyPay: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Kafka: `localhost:9092`

## Tests

Run the full suite:

```bash
mvn test
```

In this Codex environment, Maven was run with a workspace-local repository cache:

```bash
mvn -Dmaven.repo.local=work/m2 test
```

Current verified result:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

## Modernization Story

LegacyPay demonstrates a staged migration:

```text
Legacy/mainframe-style payment logic
        |
        v
Legacy adapter boundary
        |
        v
Modern Spring Boot REST API
        |
        v
Transactional payment service
        |
        v
PostgreSQL persistence with Flyway
        |
        v
Audit, metrics, and payment events
        |
        v
Kafka integration for downstream systems
```

The core payment transaction stays database-first. Kafka is used for downstream event delivery, not as the source of truth for balance changes.

## Known Local Environment Notes

Automated tests use H2 in PostgreSQL compatibility mode.

This Codex host did not have Docker available on the path, so container startup could not be verified here. PostgreSQL 16 binaries were installed through Homebrew during the final build, but this sandbox blocked PostgreSQL shared-memory initialization. The project is configured for normal local PostgreSQL and Docker Compose execution outside that sandbox limitation.
