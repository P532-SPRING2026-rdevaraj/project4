# Resource Planning Ledger — CSCI-P532 Project 4 (Week 1)

[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-Render-46E3B7?logo=render&logoColor=white)](https://YOUR-SERVICE.onrender.com)

A Spring Boot service for planning, executing, and auditing operational work
plans backed by a double-entry resource ledger. Built on the four-layer
architecture (Client → Manager → Engine → ResourceAccess) with the four
required design patterns: **State**, **Composite**, **Iterator**, and
**Template Method** (plus a Visitor extension that primes the Week-2 metric
visitors).

## 🌐 Live Demo

**Frontend + API:** https://YOUR-SERVICE.onrender.com  *(replace after deploy)*

The single-page UI is served by Spring Boot at the root path. The same URL
also serves the REST API under `/api/*`. No separate frontend deploy is
needed — the static assets in `src/main/resources/static/` are bundled into
the JAR.

---

## Quick start

### Local with Docker Compose
```bash
docker compose up --build
# http://localhost:8080
```

### Standalone container (already built JAR)
```bash
mvn -q package -DskipTests
docker build -t rpl .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/rpl \
  -e SPRING_DATASOURCE_USERNAME=rpl \
  -e SPRING_DATASOURCE_PASSWORD=rpl \
  rpl
```

### Tests
```bash
mvn test
```
32 JUnit-5 / Mockito unit tests, no `@SpringBootTest`.

---

## Architecture

| Layer            | Stereotype           | Responsibility                                 |
| ---------------- | -------------------- | ---------------------------------------------- |
| Client           | `@RestController`    | HTTP routing only; zero business logic         |
| Manager          | `@Service`           | Orchestrates use-case sequences                |
| Engine           | `@Service` / POJO    | Encapsulates one replaceable algorithm         |
| ResourceAccess   | `@Repository`        | Atomic JPA verbs; no SQL in callers            |

See [`docs/design.md`](docs/design.md) for the UML, knowledge-vs-operational
split, and Week-2 change analysis.

## Design patterns (one paragraph each)

**State.** Lives in `engine/state`. Every concrete `ActionState`
(`ProposedState`, `SuspendedState`, `InProgressState`, `CompletedState`,
`AbandonedState`) is a stateless `@Component` singleton; mutation flows
through `ActionContext`. Adding a new state in Week 2 is a one-class change.

**Composite.** Lives in `engine/composite`, with `Plan` and `ProposedAction`
implementing `PlanNode`. `Plan#getStatus()` and
`getTotalAllocatedQuantity()` are derived recursively, so leaves and
sub-plans answer the same questions and report code never branches.

**Iterator.** `engine/iterator/DepthFirstPlanIterator` is pure Java — no
JPA inside `next()`. The `PlanManager` loads the subtree first, then the
report engine and dependency-check logic walk it uniformly.

**Template Method.** `engine/ledger/AbstractLedgerEntryGenerator` defines a
`final generateEntries` skeleton that enforces double-entry conservation
inside a `final postEntries`. The Week-1 `ConsumableLedgerEntryGenerator`
only overrides `selectAllocations` and `validate`. Week 2's asset generator
will be a single new subclass overriding the empty `afterPost` hook — base
class unchanged.

**Visitor.** `PlanNode#accept(PlanNodeVisitor)` is already wired in so the
Week-2 metric visitors require *zero* edits to existing node classes.

**Posting rule.** `engine/posting/PostingRuleEngine` watches each pool
account; an entry that drops the balance below zero appends an alert entry
to a dedicated `ALERT_MEMO` account and writes an audit row.

---

## API surface (Week 1)

| Method | Path | Description |
| --- | --- | --- |
| `GET/POST` | `/api/protocols`             | List / create protocols |
| `GET/POST` | `/api/resource-types`        | List / create resource types |
| `POST`     | `/api/plans`                 | Create plan from protocol or scratch |
| `GET`      | `/api/plans/{id}`            | Plan tree with derived statuses |
| `GET`      | `/api/plans/{id}/report`     | Depth-first summary report |
| `POST`     | `/api/actions/{id}/implement`| Trigger `implement()` |
| `POST`     | `/api/actions/{id}/complete` | Trigger `complete()` |
| `POST`     | `/api/actions/{id}/suspend`  | Trigger `suspend()` |
| `POST`     | `/api/actions/{id}/resume`   | Trigger `resume()` |
| `POST`     | `/api/actions/{id}/abandon`  | Trigger `abandon()` |
| `POST`     | `/api/actions/{id}/allocations` | Attach a `ResourceAllocation` |
| `GET`      | `/api/accounts`              | All accounts with balances |
| `GET`      | `/api/accounts/{id}/entries` | Ledger entries for an account |
| `GET`      | `/api/audit-log`             | Audit trail (alerts, etc.) |

Illegal state transitions return HTTP **409 Conflict** with
`{"error":"illegal_state_transition", ...}`.

---

## UI

The single-page UI (`src/main/resources/static/`) provides:
- **Dashboard** — pool balances with red-card alerts for over-consumed pools.
- **Protocols / Resource Types** — CRUD forms backing F1/F2.
- **Plans** — collapsible composite tree, click any leaf to open the action
  detail panel. Lifecycle buttons are enabled only for legal transitions and
  the Plan-vs-Reality diff shows fields once an action is implemented.
- **Ledger** — per-account entry list with both `bookedAt` and `chargedAt`.
- **Audit log** — chronological event stream.

---

## Render.com deployment

1. Push this repo to GitHub.
2. **PostgreSQL**: in the Render dashboard → **New → PostgreSQL** (free tier,
   region near you). After it provisions, copy the **Internal Database URL**.
3. **Web Service**: **New → Web Service** → connect this repo → choose
   **Docker** environment. Port: **8080**. Plan: free.
4. Under the service's **Environment** tab add:
   - `SPRING_DATASOURCE_URL` = the Internal Database URL prefixed with
     `jdbc:` (e.g. `jdbc:postgresql://...internal/rpl`)
   - `SPRING_DATASOURCE_USERNAME` = the user from the DB page
   - `SPRING_DATASOURCE_PASSWORD` = the password from the DB page
5. **Deploy hooks**: in the Web Service settings copy the **Deploy Hook URL**
   and add it to GitHub as the repo secret `RENDER_DEPLOY_HOOK`. The
   workflow's `deploy` job will trigger Render on every push to `main`.

---

## Project layout

```
src/main/java/edu/iu/p532/rpl/
├── Application.java
├── client/                      ← @RestController layer
├── manager/                     ← @Service orchestration
├── engine/
│   ├── state/                   ← State pattern
│   ├── composite/               ← PlanNode + Visitor
│   ├── iterator/                ← Depth-first iterator
│   ├── ledger/                  ← Template Method
│   └── posting/                 ← Posting-rule engine
├── resourceaccess/              ← Spring Data JPA repositories
├── domain/                      ← JPA entities + enums
├── dto/                         ← request/response records
├── exception/                   ← typed exceptions + handler
└── config/                      ← Clock bean (deterministic tests)
```
# project4
