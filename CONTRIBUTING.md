# Contributing

LegacyPay is a learning project, so changes should keep the modernization story clear and beginner-friendly.

## Workflow

1. Keep changes focused on one concept or milestone.
2. Preserve existing API contracts unless the change explicitly requires a new contract.
3. Add or update tests for every behavior change.
4. Run the full Maven test suite before handing off:

```bash
mvn test
```

## Local Development

Use Java 21. Automated tests use H2, while the application runtime is configured for PostgreSQL through environment variables.

Do not commit real credentials. Use `.env` locally and keep `.env.example` as the documented template.

## Code Style

- Keep business logic readable.
- Prefer simple Spring components over premature abstractions.
- Keep payment state changes inside transactional boundaries.
- Keep sensitive values out of logs, audit records, tests, and documentation.
