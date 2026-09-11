# Savings Wallet

Full Stack application for managing savings goals ("bolsillo de ahorro programado"): create goals, register contributions and receive reactive completion notifications via Server-Sent Events.

## Funcionalidades

- Create savings goals (name, target amount, 3-letter currency code).
- List goals on a dashboard with progress percentage, amounts and ACTIVE / COMPLETED status.
- Register contributions against an active goal.
- Domain validation: required name, target > 0, contribution > 0, contribution cannot exceed the remaining amount, a completed goal rejects new contributions, `accumulatedAmount` never exceeds `targetAmount`.
- A goal transitions to `COMPLETED` when contributions reach exactly 100% of the target, emitting a `GoalCompleted` domain event.
- Realtime celebration dialog ("¡Meta completada!") in the UI when a goal completes, driven by SSE.
- Multi-user model (`userId` ownership, per-user event scoping); the MVP uses a demo user (`X-User-Id: 1`) with no login.

## Arquitectura

Modular Monolith with Hexagonal Architecture principles; DDD applied where it adds value:

- **Domain** — `SavingsGoal` aggregate root, `Money` value object (`BigDecimal`, never `double`/`float`), `GoalCompleted` domain event. Business rules live here and nowhere else.
- **Application** — use cases (`CreateSavingsGoal`, `GetSavingsGoals`, `AddContribution`) coordinating domain + ports (`SavingsGoalRepository`, `DomainEventPublisher`).
- **Infrastructure / Adapters** — REST controller (inbound adapter), JPA persistence adapter, SSE publisher (outbound adapters). JPA entities are kept separate from domain entities.

Patterns used: **Repository** (persistence port + JPA adapter), **Observer / Publish-Subscribe** (`GoalCompleted` → SSE subscribers), **Adapter** (REST/JPA/SSE at the edges).

> Deep dive: [docs/arquitectura.md](docs/arquitectura.md).

## Realtime

- **REST** → commands and queries (create goal, list goals, add contribution).
- **SSE** → server-to-client event notifications (`goal-completed` over `GET /api/v1/savings-goals/events?userId={userId}`).

SSE was chosen over WebSockets because the requirement is strictly one-way (server → client) push. SSE runs over plain HTTP, needs no protocol upgrade or bidirectional framing, reconnects natively in the browser via `EventSource`, and matches the notification use case without the operational cost of a socket layer.

Completion flow:

```text
Angular → POST contribution → Controller → Use Case → SavingsGoal
→ Repository → SQLite → GoalCompleted → SSE → Angular → Completion Dialog
```

The frontend keeps a single SSE stream in the App shell (survives navigation between Dashboard and the contribution form) and maps each event to a celebration dialog.

## Stack tecnológico

Backend (`apps/backend/savings-wallet`):

- Java 21, Spring Boot 3.5.16, Spring Web MVC, Spring Data JPA, Hibernate, Bean Validation
- SQLite (`sqlite-jdbc`, `hibernate-community-dialects`)
- Server-Sent Events (`SseEmitter`)
- JUnit 5, Mockito (via `spring-boot-starter-test`)

Frontend (`apps/frontend/savings-wallet-ui`):

- Angular 22 (standalone components, lazy routes), TypeScript strict, RxJS 7.8, Signals
- Angular Router, HttpClient, Reactive Forms, SCSS
- Realtime via native `EventSource` (no extra SSE/WebSocket libraries)
- Vitest via `ng test`

## Estructura del proyecto

```text
.
├── apps/
│   ├── backend/savings-wallet/          # Spring Boot app (Maven, mvnw + mvnw-jdk21)
│   │   └── src/
│   │       ├── main/java/com/example/savingswallet/
│   │       │   ├── application/
│   │       │   │   ├── port/out/              # SavingsGoalRepository, DomainEventPublisher
│   │       │   │   └── usecase/               # CreateSavingsGoal, GetSavingsGoals, AddContribution
│   │       │   ├── domain/{event,money,savingsgoal} # SavingsGoal, Money, GoalCompleted
│   │       │   └── infrastructure/{config,persistence/jpa,rest,sse}
│   │       ├── main/resources/application.properties
│   │       └── test/ ...                # unit + integration tests
│   └── frontend/savings-wallet-ui/      # Angular app (npm, ng CLI)
│       └── src/app/
│           ├── core/{models,services}   # API contracts, Api/Event services
│           └── features/savings-goals/{components,pages,services}
├── docs/
│   └── arquitectura.md
└── packages/                            # (empty, reserved)
```

(`apps/frontend/apps/` exists but is an empty placeholder; the README scaffold at `apps/frontend/savings-wallet-ui/README.md` is the untouched Angular CLI default.)

## Requisitos

- JDK 21 (the wrapper script pins `/usr/lib/jvm/java-21-openjdk-amd64`; verified present here).
- Node compatible with Angular 22 (CLI requires `^22.22.3 || ^24.15.0 || >=26`; this environment uses Node v22.23.2 via nvm).
- npm 10.9.8 (`packageManager` field in `package.json`).

Backend runs on port `8080` (SQLite file at `data/savings-wallet.db`); frontend dev server on port `4200` with a dev proxy forwarding `/api/**` to the backend (see `proxy.conf.js`).

## Ejecución

### Backend

```bash
cd apps/backend/savings-wallet
./mvnw-jdk21 spring-boot:run
```

The wrapper script pins `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (see `mvnw-jdk21`).

Backend listens on `http://localhost:8080`; SQLite file at `data/savings-wallet.db`.

### Frontend

```bash
cd apps/frontend/savings-wallet-ui
npm install
npm start        # ng serve → http://localhost:4200
```

`ng serve` uses `proxy.conf.js` (wired in `angular.json`), forwarding `/api/**`
to `http://localhost:8080` so the browser stays same-origin and avoids CORS.

## API

| Método | Endpoint                                            | Descripción                                   |
| ------ | --------------------------------------------------- | --------------------------------------------- |
| GET    | `/api/v1/savings-goals`                             | List goals (filtered by `X-User-Id` header)   |
| POST   | `/api/v1/savings-goals`                             | Create a goal (returns `201` + `Location`)    |
| POST   | `/api/v1/savings-goals/{goalId}/contributions`      | Register a contribution against an active goal |
| GET    | `/api/v1/savings-goals/events?userId={userId}`      | SSE stream of `goal-completed` events          |

UI routes: `/savings-goals` (dashboard), `/savings-goals/new` (creation form),
`/savings-goals/:goalId/contribute` (contribution form).

## Demo

Recommended flow to demonstrate the application:

1. Open `http://localhost:4200` (redirects to `/savings-goals`).
2. Click "Nueva meta" and create a goal (name, target amount, 3-letter currency).
3. See it appear on the Dashboard with its progress.
4. Click "Contribuir" on the goal and register a partial contribution.
5. See the progress update immediately (REST response updates the state).
6. Register the final contribution reaching exactly 100% → status `COMPLETED`.
7. The "¡Meta completada!" celebration dialog appears via the SSE event
   (the stream survives navigation because it lives in the App shell).

## Testing

Backend (from `apps/backend/savings-wallet`):

```bash
./mvnw-jdk21 test
```

Frontend (from `apps/frontend/savings-wallet-ui`):

```bash
ng test --watch=false
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json
ng build
```

Current suites: 110 backend `@Test` annotations (unit + integration across
domain, use cases, JPA adapters, REST and SSE); 67 frontend specs
(API/Event/State services, dashboard, forms, dialog, cards, shell routing).

## Decisiones y evolución

Deep-dive documentation:

- Architectural decisions and trade-offs: [docs/arquitectura.md](docs/arquitectura.md).
- `docs/ia.md` is referenced here as a placeholder for the AI-assisted
  development log; the file does not exist yet in the repository.

## Limitaciones conocidas / producción

Deliberate MVP scope decisions (not defects):

- **SQLite** for persistence (file-based, zero-setup); the repository port
  allows swapping the database without touching the domain.
- **No authentication**: identity comes from the `X-User-Id` header (demo user
  `1`); production identity would come from a security context.
- **In-process SSE publisher** with **best-effort delivery**: events go only to
  currently-connected subscribers of that instance; no replay, no durable event
  store. A multi-instance deployment would need a shared broker/fan-out.
- **No durable event delivery / outbox**: if a subscriber is offline when the
  goal completes, the notification is lost (the persisted `COMPLETED` state
  itself is never lost).
- **Concurrency**: concurrent contributions against the same goal would need
  pessimistic locking or optimistic versioning for production; the MVP relies on
  single-request transactional boundaries.

## License

No license file is present in the repository (checked for
`LICENSE*`/`LICENCE*`/`COPYING*`); no license is documented here.
