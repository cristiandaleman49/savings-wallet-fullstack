# Savings Wallet — Frontend Project Rules

These rules apply to all frontend work in this repository (`savings-wallet-ui`). They complement, and never override, the repository-level conventions.

## Stack

- **Angular 22** with standalone components and current Angular APIs.
- **TypeScript** in strict mode.
- **RxJS** for asynchronous streams, HTTP and SSE.
- **Angular Signals** for local reactive UI/application state.
- **SCSS** for styles (project schematics default to `scss`).
- **REST API** for request/response communication and **Server-Sent Events (SSE)** for server-to-client push.
- **Vitest** via `ng test` for unit tests.

## Source Layout

Feature-oriented structure under `src/app/`:

```
src/app/
  core/
    models/       # Shared/interfaces: API contracts, domain models, event payloads
    services/     # Cross-cutting services: HTTP API client, SSE client, config
    config/       # App constants, DI tokens, environment-related config
  features/
    savings-goals/
      components/ # Presentational components scoped to the feature
      pages/      # Route-level components that compose feature components + services
      services/   # Feature business/application logic and state
  shared/
    components/   # Reusable UI primitives (buttons, dialogs, cards, etc.)
```

- Keep **core infrastructure concerns** under `core/`.
- Keep **savings-goals functionality isolated** under `features/savings-goals/`.
- Reusable, feature-agnostic **UI primitives** belong under `shared/components/`.
- Do not scatter feature logic across the app; each feature owns its services, pages and components.

## Responsibilities

- **Components** focus on presentation and user interaction. No business orchestration in templates or component bodies.
- **Business/application orchestration** lives in services, not templates.
- **API communication is isolated behind services.** Components must never call `HttpClient` or `fetch` directly.
- **SSE communication is isolated behind a dedicated service.** Only that service manages the `EventSource`/SSE connection lifecycle and exposes observable streams.
- Backend stays the source of truth: do not duplicate backend business rules in the frontend.

## State and Data Flow

- Use **Angular Signals** for local reactive UI/application state (e.g., loading flags, derived view state, dialog visibility) and for state held by feature services.
- Use **RxJS** for asynchronous streams, HTTP requests, SSE event streams, and any `toSignal` bridging that needs stream composition.
- Favor explicit, uni-directional flow: services produce state → components read/signals → user actions call service methods.
- Avoid over-coupling: a service exposing state should expose update methods rather than letting components mutate its internals.

## API Contracts

- Define **explicit interfaces/types for API contracts** in `core/models/` (or the feature model file if feature-scoped).
- Never type API payloads as `any`. If a payload shape is unknown or partial, document it in the interface/type and handle it safely.
- Keep the API contract types aligned with the backend OpenAPI/contract documentation.

## TypeScript & Code Quality

- Strict TypeScript at all times. Avoid `any` unless technically unavoidable; if used, document why at the declaration site and narrow the type as soon as possible.
- `strictNullChecks` friendly code: no unsound non-null assertions without a documented reason.
- Prefer small, focused, single-purpose components and services.
- Follow KISS, DRY, YAGNI and separation of concerns pragmatically — avoid unnecessary abstractions and premature generalization.

## Third-Party Libraries

- Do **not** introduce NgRx, Akita, Axios, Angular Material, Tailwind, Bootstrap, or other third-party UI/state libraries unless explicitly justified in the code review.
- Prefer Angular built-in capabilities and the dependencies already declared in `package.json`.

## Validation & Error Handling

- Frontend validation is a **UX improvement only** and must never replace backend validation.
- Handle **HTTP errors explicitly**: map failures to actionable messages and present useful feedback to the user (e.g., toast, inline error state).
- Assume failures (network, 4xx, 5xx, malformed payloads) are possible and code for them.
- Do not silently swallow errors; log/route them consistently.

## Realtime (SSE) Requirements

- The dashboard must update **immediately** when a savings goal changes.
- SSE is **server-to-client only** — never send client-to-server data over the SSE connection.
- A **completed savings goal** event must trigger a **distinctive completion dialog**.
- **No polling** when SSE satisfies the requirement.
- Only the dedicated SSE service creates/owns the connection; it should expose typed observables per event type (e.g., goal created, goal updated, goal completed) and manage reconnect/error handling.

## Testing

- Write unit tests for **services** (API service, SSE service, feature services) and for **important UI behavior** (signal-driven components, completion dialog wiring).
- Prefer behavior-observing tests (fake HTTP via `provideHttpClientTesting`, fake SSE event emission, signal value assertions) over implementation detail tests.
- Run tests with `ng test`. Keep tests fast and deterministic — no real network/timing dependence.

## Workflow

- **Do not automatically commit changes.** Commits are decided by the developer.
- **AI proposes; the developer reviews and decides.** When in doubt, ask instead of assuming.
- Keep changes scoped and reviewable.