# Savings Wallet — Project Rules

## Decision ownership
- **AI proposes; the developer decides.**
- Cline implements approved tasks; the developer retains final architectural decisions.
- Do not create commits automatically.

## Architecture
- Build a modular monolith; do not introduce microservices.
- Follow Hexagonal Architecture with explicit `domain`, `application`, `adapters`, and `infrastructure` boundaries.
- Keep the domain independent of Spring, JPA, Hibernate, SQLite, HTTP, and framework annotations.
- Apply Dependency Inversion: application and domain depend on abstractions; adapters/infrastructure provide implementations.
- Put business rules and invariants in the domain.
- Keep controllers thin: validate/translate requests and delegate use cases.
- Use Repository Pattern for persistence ports and Adapter Pattern at system boundaries.
- Use Observer / Publish-Subscribe for domain/application events when events are needed.
- Use SSE only for server-to-client real-time communication.

## Design principles
- Apply SOLID pragmatically.
- Prefer KISS, DRY, YAGNI, Separation of Concerns, encapsulation, strong typing, immutability where appropriate, and testability.
- Do not add interfaces, factories, services, abstractions, or layers without a concrete need.
- Keep changes small, incremental, and scoped to the assigned task.

## Data and persistence
- Use `BigDecimal` for money; never use `double` or `float`.
- SQLite is the relational/SQL database for the MVP.
- Keep persistence design portable enough to evolve to PostgreSQL later.
- Do not couple the domain to SQLite-specific APIs or schema behavior.

## Scope constraints
- Do not add Kafka, Redis, CQRS, Event Sourcing, microservices, or authentication in the MVP.
- Do not add dependencies without explicit justification.
- Do not modify files outside the task scope.

## Testing
- Business rules require automated tests.
- Prefer fast domain tests; add application, adapter, and integration tests only where they add confidence.