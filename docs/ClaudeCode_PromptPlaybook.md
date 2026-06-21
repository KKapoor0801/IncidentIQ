# IncidentIQ — Claude Code Prompt Playbook

**Purpose:** A ready-to-use, phase-by-phase set of prompts for driving Claude Code through implementing IncidentIQ. Each phase is self-contained, references the binding SDD (`IncidentIQ_SDD.md` v1.1, MVP1 Scope), and is scoped to one focused build session.

**How to use this:**
1. Start a new Claude Code session per phase (or per sub-phase, if you split further).
2. Attach/paste `IncidentIQ_SDD.md` alongside the phase prompt — the prompt references section numbers but doesn't repeat the full content.
3. Copy the **Prompt** block for that phase verbatim (edit only where marked `[CUSTOMIZE]`).
4. Each prompt ends with an instruction telling Claude Code to ask clarifying questions before writing code if anything is ambiguous — keep that line in.
5. Work the phases in order — each one depends on artifacts from the prior phase(s), noted under **Depends On**.

---

## Phase 0 — Repo Scaffolding & Infra Skeleton

**Depends on:** Nothing (first phase)
**SDD Sections:** 2 (Folder Structure), 16 (Docker Containers), 28 (Configuration Files), 29 (application.yml)
**Goal:** Establish the monorepo skeleton and full local-dev infra, with every container booting and able to talk to each other — but zero business logic.

### Prompt

```
You are setting up the foundational monorepo skeleton for IncidentIQ, an AI-powered
Incident Intelligence Platform. This is Phase 0 of a multi-phase build. Do NOT write
any business logic, controllers, services, or domain entities in this phase — that
comes in later phases. This phase is infrastructure and scaffolding only.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 2 (Folder Structure) — exact monorepo layout to create
- Section 16 (Docker Containers) — exact container list, base images, ports
- Section 28 (Configuration Files) — what config files exist per service
- Section 29 (application.yml) — exact YAML structure for the Spring Boot config

TASKS:
1. Create the monorepo directory structure exactly as specified in Section 2,
   including backend-core/, ai-service/, frontend/, infra/, and docs/.
2. Initialize backend-core/ as a Spring Boot 4 (Java 21) Maven project with a
   minimal pom.xml (Spring Web, Spring Data JPA, Spring Data Redis, Spring Data
   Elasticsearch, Spring Kafka, Spring Security, Flyway, PostgreSQL driver,
   springdoc-openapi, Lombok, JUnit 5). Empty IncidentIqApplication.java that
   boots successfully.
3. Initialize ai-service/ as a Python FastAPI project (Python 3.12) with
   pyproject.toml / requirements.txt including: fastapi, uvicorn, pydantic,
   kafka-python or aiokafka, httpx, tenacity, structlog, mypy, black, isort, ruff.
   A minimal main.py with a /health endpoint only.
4. Initialize frontend/ as a React + Vite + TypeScript project (strict mode)
   with the folder structure from Section 2. Default Vite starter page only.
5. Write infra/docker-compose.yml defining ALL containers from Section 16
   (frontend, core, ai, postgres, redis, elasticsearch, kafka, zookeeper, ollama)
   on a shared bridge network "incidentiq-net" with named volumes for
   postgres-data, esdata, ollama-models. Use the exact image tags from Section 16.
6. Write application.yml, application-dev.yml, application-prod.yml for
   backend-core following the exact structure in Section 29 (placeholders for
   secrets, not real values).
7. Write .env.example for ai-service and docker-compose with all required env
   vars referenced in Sections 28-29 (no real secrets).
8. Write a root README.md explaining how to boot the full stack locally with
   docker-compose, and how to run each service independently for development.

ACCEPTANCE CRITERIA:
- `docker-compose up` brings up all 9 containers without crash-looping.
- Spring Boot app boots on :8080 and responds on an actuator health endpoint.
- FastAPI responds on :8000/health.
- React dev server boots on :5173 (Vite default) showing a placeholder page.
- No business logic, no domain entities, no controllers beyond health checks.

If anything about the folder structure, dependency versions, or container
configuration is ambiguous or underspecified in the SDD, STOP and ask me before
proceeding rather than guessing.
```

---

## Phase 1 — Database Schema & Domain Model (Spring Boot)

**Depends on:** Phase 0
**SDD Sections:** 3 (Package Structure — `domain/`, `repository/jpa/`), 4 (DB Schema), 5 (ERD), 25 (Naming Conventions)
**Goal:** Flyway-managed schema, JPA entities, enums, and Spring Data repositories. No services, no controllers, no REST surface yet.

### Prompt

```
This is Phase 1 of the IncidentIQ build (Spring Boot core service only). Phase 0
already scaffolded the repo and docker-compose stack — assume it exists. This
phase ONLY covers the persistence layer: schema migrations, JPA entities,
enums, and repositories. Do NOT write services, controllers, DTOs, or any REST
endpoints in this phase.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 4 (Database Schema) — exact tables, columns, types, constraints for
  users, incidents, incident_history, incident_comments
- Section 4.2 (Indexes) — exact indexes to create
- Section 4.3 (Migration Strategy) — Flyway, no manual DDL
- Section 5 (ERD) — relationships between entities
- Section 3 (Package Structure) — exact package layout for domain/entity,
  domain/enums, repository/jpa
- Section 25 (Naming Conventions) — Java class/method naming, DB snake_case

TASKS:
1. Write Flyway migration V1__init_schema.sql creating all four tables exactly
   as specified in Section 4.1, with all constraints, defaults, and the
   indexes from Section 4.2. Use gen_random_uuid() for UUID PKs (enable the
   pgcrypto extension if needed).
2. Create JPA entities in com.incidentiq.core.domain.entity matching the
   schema 1:1: Incident, User, IncidentHistory, IncidentComment. Use
   Lombok for boilerplate. Add @Version on Incident for optimistic locking
   (referenced later in Section 24.1).
3. Create enums in com.incidentiq.core.domain.enums: IncidentStatus
   (OPEN, IN_PROGRESS, RESOLVED, CLOSED), IncidentPriority (P1-P4),
   IncidentCategory (PAYMENTS, AUTH, INFRA, DATABASE, NETWORK, UNKNOWN —
   confirm this taxonomy is fine, it's pulled from Section 15.1 step 3),
   Role (ADMIN, ENGINEER, VIEWER).
4. Create Spring Data JPA repositories in
   com.incidentiq.core.repository.jpa: IncidentRepository, UserRepository,
   IncidentHistoryRepository — interfaces only, standard derived queries
   plus pagination support (Pageable) on IncidentRepository.
5. Wire the entity relationships per the ERD (Section 5): Incident -> many
   IncidentHistory, Incident -> many IncidentComment, User -> many Incident
   (as reporter), User -> many Incident (as assignee).
6. Write a small Flyway-driven integration test (or @DataJpaTest) confirming
   the schema applies cleanly and basic CRUD works for each entity.

ACCEPTANCE CRITERIA:
- `mvn flyway:migrate` (or app boot with flyway.enabled=true) applies V1
  cleanly against the dockerized Postgres.
- All four entities map correctly with no Hibernate validation errors
  (ddl-auto: validate per Section 29 should pass against the Flyway schema).
- Repositories compile and basic save/find round-trips work in tests.

If the embedding_vector_id column's nullability/usage, the exact CHECK
constraint syntax, or any FK cascade behavior is unclear from the SDD, ask me
before making an assumption — don't silently pick a default.
```

---

## Phase 2 — Auth & JWT (Spring Boot)

**Depends on:** Phase 1
**SDD Sections:** 13 (Auth Flow), 14 (JWT Flow + RBAC), 30 (Security Rules, items 1–6), 3 (`security/` package)
**Goal:** Full login/register/refresh/logout cycle with JWT issuance, Redis-backed blacklist/refresh tracking, and RBAC enforcement scaffolding.

### Prompt

```
This is Phase 2 of the IncidentIQ build (Spring Boot core service). Phases 0-1
are complete: infra is up, schema/entities/repositories exist. This phase
covers authentication and authorization end-to-end. Do NOT build incident
CRUD endpoints yet — only auth.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 13 (Authentication Flow) — the full login -> token issuance ->
  refresh -> logout lifecycle, step by step
- Section 14.1 (Token Structure) — exact JWT claims for access and refresh
  tokens
- Section 14.2 (JWT Flow Diagram) — the sequence to implement precisely,
  including Redis interactions at each step
- Section 14.3 (RBAC) — VIEWER / ENGINEER / ADMIN permission model
- Section 6.1 — auth endpoint signatures: POST /api/v1/auth/register,
  POST /api/v1/auth/login, POST /api/v1/auth/refresh
- Section 7.1 (LoginRequest DTO), 7.2 (AuthResponse DTO)
- Section 30 (Security Rules) items 1-6 — transport, authentication,
  authorization, password storage (BCrypt cost 12), secrets management, CORS
- Section 3 — exact classes to create in security/ and controller/

TASKS:
1. Implement JwtTokenProvider (com.incidentiq.core.security) generating and
   validating access tokens (15 min expiry) and refresh tokens (7 day
   expiry) with the exact claims from Section 14.1, signed HS256.
2. Implement JwtAuthenticationFilter validating signature, expiry, and Redis
   blacklist (auth:jwt:blacklist:{jti}) on every request, per Section 14.2.
3. Implement CustomUserDetailsService backed by UserRepository.
4. Implement AuthService + AuthController:
   - POST /api/v1/auth/register — creates user, BCrypt cost 12, role
     defaults to ENGINEER unless specified (confirm this default with me
     if you think it should differ).
   - POST /api/v1/auth/login — validates credentials, issues token pair,
     stores refresh token reference in Redis (auth:refresh:{userId}) per
     Section 14.2.
   - POST /api/v1/auth/refresh — validates refresh jti against Redis,
     rotates it (delete old, store new), issues new access token.
   - POST /api/v1/auth/logout — blacklists access token jti in Redis with
     TTL = remaining token life, deletes refresh token reference.
5. Implement SecurityConfig wiring the filter chain: public endpoints
   (/auth/register, /auth/login, /auth/refresh) open, everything else under
   /api/v1/** requires valid JWT. CORS allow-list from an env var per
   Section 30 item 6 (no wildcard).
6. Add @PreAuthorize-ready role checks (RBAC table from Section 14.3) as a
   reusable pattern/annotation — even though no business endpoints exist
   yet, set up the convention other phases will follow.
7. Map auth failures to the standard error envelope (you'll find the full
   envelope spec in Section 21 — implement just enough of
   GlobalExceptionHandler to cover 401/403/409 cases relevant to auth here;
   full exception handling is formalized in a later phase).

ACCEPTANCE CRITERIA:
- Register -> login -> authenticated request -> refresh -> logout works
  end-to-end against the dockerized Postgres + Redis.
- Expired/blacklisted/tampered tokens are rejected with 401.
- Passwords are never returned in any response and never logged.
- Refresh token rotation actually invalidates the old refresh token.

If the default role on registration, the HS256 vs RS256 signing choice, or
how strictly to enforce RBAC at this stage (since no business endpoints
exist yet to protect) is unclear, ask me rather than assuming.
```

---

## Phase 3 — Incident CRUD, Comments & History (Spring Boot)

**Depends on:** Phase 2
**SDD Sections:** 6.1 (incidents/comments/history endpoints), 7.1–7.2 (DTOs), 22 (Validation), 21 (Error Responses), 24.1 (Exception Handling), Appendix A (state machine)
**Goal:** The full transactional core — incident lifecycle, comments, audit history — fully validated and RBAC-protected.

### Prompt

```
This is Phase 3 of the IncidentIQ build (Spring Boot core service). Auth
(Phase 2) is in place. This phase builds the core transactional CRUD surface
for incidents, comments, and history. Do NOT touch Kafka, Redis caching, or
Elasticsearch in this phase — those are later phases. Persist to Postgres only
for now; Kafka event publishing will be wired in during Phase 6.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 6.1 — exact endpoint list: POST/GET/PUT/PATCH/DELETE on
  /api/v1/incidents, /api/v1/incidents/{id}/comments,
  /api/v1/incidents/{id}/history
- Section 7.1 (Request DTOs) — CreateIncidentRequest, UpdateIncidentRequest,
  ResolveIncidentRequest, AddCommentRequest — exact field constraints
- Section 7.2 (Response DTOs) — IncidentResponse, UserSummary, PageResponse<T>
- Section 22 (Validation Rules) — full validation table, including the
  state-transition rule for status (OPEN -> IN_PROGRESS -> RESOLVED ->
  CLOSED, no backward skip except ADMIN override)
- Appendix A (Incident State Machine) — the exact transition diagram to
  enforce
- Section 21 (API Error Responses) — standard error envelope (21.1), full
  error catalog (21.2), validation error detail format (21.3)
- Section 24.1 (Exception Handling — Spring Boot) — GlobalExceptionHandler,
  custom exception hierarchy, optimistic locking -> 409
- Section 14.3 (RBAC) — VIEWER read-only, ENGINEER create/update/resolve/
  comment, ADMIN delete/close + override transitions
- Section 26 — coding standards: constructor injection only, services
  return DTOs never entities, no business logic in controllers

TASKS:
1. Create request/response DTOs in dto/request and dto/response exactly per
   Sections 7.1-7.2, with Jakarta Bean Validation annotations
   (@NotBlank, @Size, etc.) matching Section 22's constraint table.
2. Create IncidentMapper (entity <-> DTO) in mapper/.
3. Implement a custom @ValidStatusTransition validator enforcing Appendix A's
   state machine, with the ADMIN-override backward-transition exception.
4. Implement IncidentService with: createIncident, updateIncident,
   resolveIncident, getIncident, listIncidents (paginated + filterable by
   status/priority/category), and comment/history operations. reporterId on
   create must be derived from the JWT principal, never trusted from the
   client body (per the DTO spec note in Section 7.1).
5. Implement IncidentController, IncidentCommentController (or fold into
   IncidentController, your call, but keep controllers thin per Section 26),
   wiring RBAC via @PreAuthorize per Section 14.3 (e.g., DELETE is
   ADMIN-only).
6. Every status/priority/assignee change must write an IncidentHistory row
   capturing old/new value and the acting userId (audit trail, referenced
   in Section 30 item 11).
7. Implement GlobalExceptionHandler (@RestControllerAdvice) mapping
   IncidentNotFoundException -> 404, ValidationException -> 422,
   OptimisticLockingFailureException -> 409, Bean Validation failures -> 400
   with the field-level error format from Section 21.3. Every response must
   use the standard envelope from Section 21.1, including traceId (a
   placeholder/random traceId is fine for now — proper MDC-based tracing
   comes in the logging phase).
8. Write integration tests covering: happy-path create/update/resolve,
   invalid status transition rejection, RBAC denial (VIEWER attempting
   create), optimistic lock conflict on concurrent update, and pagination.

ACCEPTANCE CRITERIA:
- All endpoints in Section 6.1 (excluding search, which is Phase 5) are live
  and pass the validation rules in Section 22 exactly.
- Illegal state transitions return 422 with a clear message; ADMIN override
  works as specified.
- incident_history rows are created correctly for every mutating field.
- Error responses match the envelope in Section 21.1/21.3 exactly.

If the exact ADMIN-override mechanism (e.g., a query param, a separate
endpoint, or a role check inline) isn't fully specified, or if you're unsure
whether DELETE should hard-delete or soft-close (the SDD says "soft-delete/
close" in Section 6.1 but the schema has no deleted_at-style column), ask me
before deciding how to implement it.
```

---

## Phase 4 — Redis Caching (Spring Boot)

**Depends on:** Phase 3
**SDD Sections:** 10 (Cache Strategy), 11 (Key Naming)
**Goal:** Cache-aside layer on top of the existing CRUD/read paths, plus dashboard aggregates and rate limiting.

### Prompt

```
This is Phase 4 of the IncidentIQ build (Spring Boot core service). Incident
CRUD (Phase 3) is complete and working against Postgres directly. This phase
adds Redis caching on top — it should not change any existing API contracts,
only their internal implementation and latency characteristics.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 10 (Redis Cache Strategy) — exact use cases, strategy
  (cache-aside vs write-through), and TTLs per use case
- Section 11 (Redis Key Naming) — exact key patterns to use, no deviation
  (e.g., incident:detail:{incidentId}, incident:list:v{version}:...)
- Section 3 — IncidentCacheService belongs in service/

TASKS:
1. Implement IncidentCacheService wrapping Redis operations for:
   - incident:detail:{incidentId} — cache-aside, write-through invalidation
     on update/resolve, TTL 10 min
   - incident:list:v{version}:{filtersHash}:{page}:{size} — cache-aside,
     TTL 30 sec
   - incident:list:version — monotonic counter bumped on ANY incident write
     (this is how list-cache invalidation works — no wildcard scans)
   - dashboard:counts:open — cache-aside, refreshed by a scheduled job
     every 60s (use Spring's @Scheduled)
2. Wire IncidentService (from Phase 3) to check cache before hitting
   Postgres on getIncident and listIncidents, and to invalidate/bump
   correctly on every write path (create/update/resolve/delete).
3. Implement Redis-backed rate limiting (ratelimit:{userId}:{endpoint}) as
   a token-bucket via INCR + EXPIRE, applied to POST /incidents and
   POST /auth/login per Section 30 item 8. Exceeding the limit returns 429
   with the RATE_LIMITED error code from Section 21.2.
4. Implement a dashboard aggregates endpoint (or confirm with me if one
   doesn't yet exist in Section 6.1 and should be added) backed by
   dashboard:counts:open and similar keys.
5. Write a hashing utility for filtersHash used in the list-cache key, and
   document the exact filter-serialization rule so cache keys are
   deterministic across requests with the same effective filters.

ACCEPTANCE CRITERIA:
- Repeated GET /incidents/{id} calls hit cache after the first (verifiable
  via logging or Redis MONITOR), and the cache entry disappears immediately
  after an update/resolve.
- List queries with identical filters/page/size return cached results
  within the 30s TTL; any incident write bumps incident:list:version,
  invalidating all previously cached list pages implicitly.
- Rate limiting kicks in correctly and resets after the rolling window.

If a dashboard/aggregates REST endpoint doesn't already exist from Phase 3
and the SDD doesn't explicitly define its contract, ask me what shape you'd
like (e.g., GET /api/v1/dashboard/summary) before inventing one.
```

---

## Phase 5 — Elasticsearch Indexing & Keyword Search (Spring Boot)

**Depends on:** Phase 3 (and ideally Phase 4)
**SDD Sections:** 12.1–12.4 (ES Mappings + Search Strategy — MVP1 keyword-only), 6.1 (`/incidents/search`, `/runbooks/search`)
**Goal:** Index incidents to ES on write, and expose keyword search over both `incidents` and `runbooks`.

### Prompt

```
This is Phase 5 of the IncidentIQ build (Spring Boot core service). This
phase is MVP1-scoped: keyword search ONLY. Do not implement any vector
search, embeddings, or dense_vector fields — those are explicitly deferred
to MVP2 per Section 0.1 / Appendix C of the SDD. If you find yourself
wanting to add semantic search "for completeness," stop and don't — it's
out of scope for this phase.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 12.1 — `incidents` index mapping (keyword fields only)
- Section 12.2 — `runbooks` index mapping
- Section 12.3 — `service_metadata` index mapping
- Section 12.4 — Search Strategy: multi_match queries, title^2 boost
- Section 6.1 — GET /api/v1/incidents/search?q=, GET /api/v1/runbooks/search?q=
- Section 3 — repository/elasticsearch/IncidentSearchRepository,
  controller/IncidentSearchController

TASKS:
1. Define the three ES index mappings (incidents, runbooks,
   service_metadata) exactly per Sections 12.1-12.3, applied at bootstrap
   (e.g., via an ApplicationRunner or an init script in
   infra/elasticsearch/mappings/).
2. Wire IncidentService (from Phase 3) to index a base incident document to
   the `incidents` ES index on create, and re-index on every update/resolve
   — keyword fields only (title, description, status, priority, category,
   reporterId, assigneeId, aiResolutionSuggestion, timestamps). This should
   NOT block the synchronous request path noticeably; if indexing needs to
   be async, use a simple @Async method for now (full Kafka-driven
   async indexing isn't required at this layer per the SDD).
3. Implement IncidentSearchRepository (Spring Data Elasticsearch) and
   IncidentSearchService.search(query, page) using multi_match against
   title (boosted ^2) and description, per Section 12.4.
4. Implement a RunbookSearchService + endpoint
   (GET /api/v1/runbooks/search?q=) using the same multi_match pattern
   against the runbooks index (title^2, body).
5. Implement IncidentSearchController exposing both search endpoints,
   RBAC-protected per Section 14.3 (any authenticated role can search).
6. Wrap search calls with the Redis cache layer from Phase 4
   (runbook:search:{queryHash}, 5 min TTL) — reuse IncidentCacheService
   conventions.
7. Note: the `runbooks` and `service_metadata` indices will be empty until
   Phase 8 (AI service) seeds them — write the search code to work
   correctly against an empty index (return empty PageResponse, not an
   error).

ACCEPTANCE CRITERIA:
- Creating/updating an incident reflects in `incidents` index search
  results within a reasonable delay (a few hundred ms is fine for MVP1).
- GET /api/v1/incidents/search?q=payment returns ranked results boosting
  title matches.
- GET /api/v1/runbooks/search?q=... returns an empty paginated result set
  gracefully when the index has no documents yet.
- No dense_vector fields, no kNN queries, no embedding-related code exists
  anywhere in this phase's output.

If you're unsure whether ES indexing on write should be synchronous or
async at this layer (since full Kafka-driven re-indexing only happens after
the AI callback, per Section 15.1 step 8, which is a later phase), ask me
rather than guessing the concurrency model.
```

---

## Phase 6 — Kafka Producer & Event Publishing (Spring Boot)

**Depends on:** Phase 3
**SDD Sections:** 8 (Kafka Topics), 9.1–9.2 (`incident.created`/`incident.updated` payloads), 3 (`kafka/producer/`)
**Goal:** Wire incident create/update flows to publish Kafka events, completing the producer side of the async AI boundary.

### Prompt

```
This is Phase 6 of the IncidentIQ build (Spring Boot core service). Incident
CRUD (Phase 3) is complete. This phase wires Kafka event publishing into the
existing create/update flows. The consumer side (FastAPI) is built in a
later phase — this phase only needs the producer to work correctly and the
topics to exist; you can verify publishing via a local console consumer or
kafka-console-consumer if needed.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 8 (Kafka Topics) — exact topic names, partition counts, retention,
  partition key (incidentId)
- Section 9.1 (`incident.created` payload) — exact JSON schema
- Section 9.2 (`incident.updated` payload) — exact JSON schema, including
  changedFields[] and requiresReprocessing flag
- Section 3 — kafka/producer/IncidentEventProducer.java
- Section 24.1 — note on Kafka publish failure handling: "incident creation
  still succeeds in Postgres even if the Kafka publish fails — publish
  failure is logged and retried via an outbox-pattern fallback"

TASKS:
1. Configure KafkaConfig: producer factory, JsonSerializer for values,
   StringSerializer for keys, acks=all, retries=3 (matches application.yml
   from Phase 0).
2. Create the four topics (incident.created, incident.updated,
   incident.ai.completed, incident.ai.dlq) with the exact partition counts
   from Section 8 — either via a KafkaAdmin bean or a docker-compose/init
   script, your choice, but document which approach you took.
3. Implement IncidentEventProducer with publishCreated(incident),
   publishUpdated(incident, changedFields), publishAiCompleted(incident) —
   constructing payloads exactly per Sections 9.1-9.3 (9.3 will actually be
   used starting Phase 9, but define the method now for completeness; don't
   wire it into anything yet).
4. Wire IncidentService.createIncident to call publishCreated AFTER the
   Postgres transaction commits (incidentId must exist before publishing).
   Key the event by incidentId per Section 8's partition key rationale.
5. Wire IncidentService.updateIncident and resolveIncident to call
   publishUpdated, correctly populating changedFields[] and setting
   requiresReprocessing=true only when title or description changed (a
   pure status-only resolve, per Section 17.3's example, should set
   requiresReprocessing=false).
6. Implement the basic outbox-pattern fallback referenced in Section 24.1:
   if the Kafka publish throws/fails, log it at ERROR and do NOT roll back
   the Postgres write. Add a simple "pending republish" mechanism — e.g., a
   boolean/flag or a lightweight outbox table — that a scheduled job can
   later pick up. Keep this minimal; full reconciliation-job design is not
   required in detail, just the hook point.
7. Write a test (using an embedded Kafka broker or Testcontainers) verifying
   that creating an incident produces a correctly-keyed, correctly-shaped
   incident.created message.

ACCEPTANCE CRITERIA:
- POST /api/v1/incidents results in a message on incident.created matching
  Section 9.1's schema exactly, keyed by incidentId.
- PUT /api/v1/incidents/{id} and PATCH .../resolve produce correctly-shaped
  incident.updated messages with accurate changedFields and
  requiresReprocessing.
- A simulated Kafka outage does not cause incident creation to fail or
  roll back — it degrades gracefully and logs the failure.

If you're unsure whether to implement the outbox fallback as a real
database-backed outbox table (more correct, more work) or a simpler
in-memory/flag-based retry for MVP1 purposes, ask me which level of rigor
you want here before building it.
```

---

## Phase 7 — FastAPI AI Service Core (Categorization & Prioritization)

**Depends on:** Phase 6 (producer must exist to test against)
**SDD Sections:** 3.1 (FastAPI Package Structure), 8 (Kafka — consumer group), 9.1–9.2, 15.1 (steps 1–4), 15.3 (Model Inventory), 26 (Python coding standards)
**Goal:** A working Kafka consumer in FastAPI that classifies incidents via Ollama. No resolution suggestion yet — that's Phase 8.

### Prompt

```
This is Phase 7 of the IncidentIQ build (Python FastAPI AI service). Phase 6
gave you a working Kafka producer on the Spring Boot side publishing to
incident.created and incident.updated. This phase builds the consumer side:
FastAPI consumes those events and calls Ollama for categorization and
prioritization ONLY. Do NOT implement resolution suggestion, Elasticsearch
keyword lookup, or the callback to Spring Boot in this phase — those are
Phases 8 and 9 respectively. This phase ends with FastAPI producing a
classification result in memory/logs, not yet sent anywhere.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 3.1 — exact FastAPI package structure to follow
- Section 8 — consumer group name "ai-intelligence-service", topics
  incident.created and incident.updated (only process incident.updated
  when requiresReprocessing=true)
- Section 9.1, 9.2 — exact event payload shapes you'll be deserializing
- Section 15.1 steps 1-4 — trigger, consume, categorize, prioritize, exactly
  as described (categorization taxonomy: PAYMENTS, AUTH, INFRA, DATABASE,
  NETWORK, UNKNOWN; prioritization outputs P1-P4 + confidence score)
- Section 15.3 — model inventory: llama3.1:8b via Ollama, structured-output
  prompting with JSON schema constraint
- Section 26 (Python/FastAPI standards) — type hints mandatory, mypy strict,
  black/isort/ruff, Pydantic models for all schemas (no raw dicts), async
  I/O for all Kafka/HTTP handlers
- Section 24.2 — failure handling pattern (retries + DLQ) — implement the
  retry mechanics now even though DLQ publishing itself is wired in Phase 9

TASKS:
1. Implement Pydantic models in models/ for IncidentCreatedEvent and
   IncidentUpdatedEvent matching Sections 9.1-9.2 exactly, plus an internal
   AiClassificationResult model (category, priority, confidenceScore,
   modelUsed, processedAt).
2. Implement clients/ollama_client.py — an async HTTP client wrapping calls
   to the local Ollama runtime (http://ollama:11434 or configured host),
   with structured JSON-schema-constrained prompting so responses are
   reliably parseable. Include a configurable timeout (referenced later as
   30000ms in Section 9.4's DLQ example) and use tenacity for retry with
   exponential backoff, max 3 attempts, per Section 24.2.
3. Implement services/categorization_service.py — prompts Ollama with
   title+description, constrained to the fixed taxonomy from Section 15.1
   step 3. Must return one of the six valid category values, never free
   text.
4. Implement services/prioritization_service.py — prompts Ollama with
   title+description+category+any severity-indicative signals you can
   reasonably derive from the text, returning P1-P4 plus a confidence
   score (0.0-1.0), per Section 15.1 step 4.
5. Implement consumers/incident_event_consumer.py — an async Kafka consumer
   in consumer group ai-intelligence-service, subscribed to both topics,
   filtering incident.updated events to only those with
   requiresReprocessing=true. On each valid event, calls categorization
   then prioritization in sequence, logs the resulting
   AiClassificationResult (no callback yet — that's Phase 9).
6. Implement structured logging (structlog) per Section 23's AI-specific
   logging note: log prompt template version, model name, and latency per
   inference call; do NOT log full prompt/response content by default
   (sampled logging at 1% mentioned in the SDD can be a TODO/config flag
   for now rather than fully built).
7. Write tests mocking the Ollama HTTP layer, verifying: correct taxonomy
   constraint enforcement, retry-then-fail behavior surfaces a clear
   exception type (OllamaTimeoutError or similar, to be caught by the
   exception handlers wired in Phase 9), and that incident.updated events
   without requiresReprocessing=true are correctly skipped.

ACCEPTANCE CRITERIA:
- Publishing a message to incident.created (e.g., via a test script or
  kafka-console-producer) results in FastAPI logging a correctly-categorized,
  correctly-prioritized result within a reasonable time.
- incident.updated events with requiresReprocessing=false are ignored.
- Simulated Ollama timeouts trigger exactly 3 retries with exponential
  backoff before raising/logging a terminal failure.
- mypy --strict and ruff pass cleanly on the new code.

If the exact Ollama prompt template format (e.g., whether you use Ollama's
native JSON mode, a custom system prompt, or function-calling-style
constraints) isn't something you're confident about producing reliable
structured output with, ask me — I'd rather you flag a risk in classification
reliability than silently ship something fragile.
```

---

## Phase 8 — Resolution Suggestion via Keyword Lookup (FastAPI) + Seed Data

**Depends on:** Phase 7
**SDD Sections:** 12.2–12.3 (runbooks/service_metadata indices + seed data), 15.1 (steps 5–6), 24.2 (FastAPI exception handling)
**Goal:** Keyword-search-driven resolution suggestions — no embeddings, no RAG framework — plus placeholder seed content to populate the `runbooks` and `service_metadata` indices.

### Prompt

```
This is Phase 8 of the IncidentIQ build (Python FastAPI AI service).
Categorization and prioritization (Phase 7) work. This phase adds the
resolution-suggestion step: a plain Elasticsearch keyword lookup followed by
a single Ollama prompt using the match as context. This is explicitly NOT a
RAG framework — no LangChain, no embeddings, no vector search, no retriever
abstraction. It's two sequential calls: an ES multi_match query, then one
Ollama prompt with the result as plain-text context. If you find yourself
reaching for a retrieval abstraction library, stop — that's MVP2 scope.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 12.2 (runbooks index mapping) and 12.3 (service_metadata index
  mapping) — what you're seeding and querying
- Section 12.4 (Search Strategy) — the multi_match pattern, title^2 boost
- Section 15.1 steps 5-6 — exact retrieve-then-prompt sequence: query
  runbooks (and optionally incidents filtered to status=RESOLVED) using the
  incident's title+description as query text, take top 1-3 hits, insert as
  plain-text context into a single Ollama prompt template
- Section 24.2 — circuit breaker (via tenacity) for any outbound calls

TASKS:
1. Implement clients/elasticsearch_client.py — an async ES client wrapping
   multi_match queries against the runbooks index (and optionally incidents
   filtered to RESOLVED), matching the query pattern from Section 12.4.
2. Implement services/resolution_suggestion_service.py:
   - Step A (keyword lookup): query runbooks using the incident's
     title+description as free text, retrieve top 1-3 hits as plain text
     (title + body).
   - Step B (prompt): construct a single Ollama prompt: "Given this
     incident: {title}/{description}, and this matched runbook: {runbook
     title}/{runbook body}, suggest a resolution." Call ollama_client (from
     Phase 7) and return the resulting suggestion text.
   - If no runbook hits are found, the service should still return a
     reasonable LLM-generated suggestion based on the incident alone (don't
     fail the whole AI workflow just because there's no keyword match) —
     confirm with me if you'd instead prefer it return null/empty in that
     case.
3. Wire incident_event_consumer.py (from Phase 7) to call
   resolution_suggestion_service AFTER categorization/prioritization,
   completing the full classification result object (category, priority,
   confidenceScore, resolutionSuggestion, modelUsed, processedAt) — still
   just logging the result for now, not calling back to Spring Boot (Phase 9).
4. Generate placeholder seed data and a seed script
   (e.g., scripts/seed_elasticsearch.py) that bulk-indexes:
   a) 10-15 runbook markdown files under a new seed-data/runbooks/
      directory. Generate realistic incident-response runbooks for an
      e-commerce/SaaS platform covering topics like: payment gateway
      timeouts, database connection pool exhaustion, auth/SSO token
      expiry storms, API rate-limit cascading failures, Redis cache
      stampede, Kafka consumer lag/backpressure, third-party webhook
      delivery failures, DNS/CDN propagation issues, memory leak/OOM
      kill loops, and SSL certificate expiry. Each runbook should have a
      title, a short symptom description, and 3-5 concrete remediation
      steps — realistic enough to be useful as LLM prompt context.
   b) A service_metadata.json seed file with ~8-10 entries (serviceName,
      owner, tier, dependencies[], description) for plausible
      microservices this platform might have (e.g., payment-service,
      auth-service, notification-service, search-service, billing-service)
      consistent with the runbook topics above.
   c) 20-30 seeded historical incidents (status=RESOLVED) written directly
      to Postgres (and thus indexed into the incidents ES index via the
      Phase 5 indexing path) — short title/description/resolutionNotes
      triples thematically aligned with the runbooks, so keyword search
      has real signal to match against.
5. Write tests verifying: a query matching a seeded runbook returns it as
   the top hit, the resolution suggestion service produces non-empty output
   for both matched and unmatched cases, and the full consumer pipeline
   (categorize -> prioritize -> resolution suggestion) runs end-to-end
   against a test incident.

ACCEPTANCE CRITERIA:
- Running the seed script populates runbooks and service_metadata indices
  with realistic content, and 20-30 RESOLVED incidents exist in Postgres/ES.
- A test incident about "checkout payment failing" surfaces the payment
  gateway timeout runbook as a top keyword match.
- The full FastAPI consumer pipeline produces a complete, logged
  classification result (category + priority + resolution suggestion) with
  no embeddings, vector fields, or RAG-framework code anywhere in the
  implementation.

Before generating the seed content, confirm with me: should the 20-30 seed
historical incidents be inserted via a Spring Boot Flyway data-migration
(V2__seed_incidents.sql) or via a standalone Python/SQL seed script run
once at FastAPI bootstrap? The SDD doesn't pin this down explicitly, and it
affects which service owns the seed data lifecycle.
```

---

## Phase 9 — Internal Callback Loop (Both Services)

**Depends on:** Phase 8 (FastAPI side), Phase 6 (Spring Boot Kafka side)
**SDD Sections:** 6.2 (Internal API), 7.3 (`AiResultCallbackRequest`), 9.3–9.4 (`incident.ai.completed`/`incident.ai.dlq`), 15.1 (steps 7–9), 24.1–24.2, 30 (item 10)
**Goal:** Close the loop — FastAPI writes results back into Spring Boot, Spring Boot persists/re-indexes/invalidates/publishes completion, and failures route to a DLQ.

### Prompt

```
This is Phase 9 of the IncidentIQ build, touching BOTH services. It's the
capstone of the AI workflow: FastAPI's classification result (Phase 8) gets
written back into Spring Boot via an internal REST callback, completing the
full async loop from Section 1.2.2 of the SDD. Build the Spring Boot side
first, then the FastAPI side, then test them together.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 6.2 (Internal APIs) — PATCH /internal/api/v1/incidents/{id}/
  ai-result, internal service token auth, excluded from public Swagger
- Section 7.3 (AiResultCallbackRequest) — exact payload shape
- Section 9.3 (incident.ai.completed) and 9.4 (incident.ai.dlq) — exact
  payload shapes
- Section 15.1 steps 7-9 — callback, persist & propagate, failure handling
  (3 retries with exponential backoff, then DLQ, incident stays
  aiProcessed=false)
- Section 24.1 — InternalServiceSecurityFilter pattern
- Section 24.2 — circuit breaker (tenacity) on the callback REST call to
  avoid hammering Spring Boot during an outage
- Section 30 item 10 — internal trust boundary: static rotateable service
  token via X-Internal-Token header, not reachable from public ingress

SPRING BOOT TASKS:
1. Implement InternalServiceSecurityFilter validating the X-Internal-Token
   header against the configured ai-callback-token (from application.yml,
   Phase 0), applied only to /internal/** paths, completely separate from
   the JWT filter chain from Phase 2.
2. Implement InternalAiCallbackController with
   PATCH /internal/api/v1/incidents/{id}/ai-result accepting
   AiResultCallbackRequest (Section 7.3).
3. Implement AiCallbackService.applyAiResult(callback):
   - Updates the incident's category, priority, aiResolutionSuggestion,
     aiConfidenceScore, sets aiProcessed=true.
   - Re-indexes the incident in Elasticsearch (keyword fields, reusing
     Phase 5's indexing path).
   - Invalidates the Redis cache for that incident (reusing Phase 4's
     IncidentCacheService).
   - Publishes incident.ai.completed via IncidentEventProducer (Phase 6),
     payload exactly per Section 9.3.
4. Exclude /internal/** from the public OpenAPI spec
   (springdoc.paths-to-exclude) — this anticipates Phase 11 but set the
   property now since you're touching OpenAPI-adjacent config.
5. Write integration tests: valid token + valid payload updates the
   incident correctly end-to-end (DB, ES, Redis, Kafka); missing/invalid
   token returns 401/403; the endpoint is unreachable via any JWT-based
   auth path.

FASTAPI TASKS:
6. Implement clients/core_service_client.py — async HTTP client issuing the
   PATCH callback to Spring Boot with the X-Internal-Token header, wrapped
   in a tenacity circuit breaker (retry + backoff) per Section 24.2.
7. Wire incident_event_consumer.py (Phase 8) to call core_service_client
   after a successful classification, completing the loop that was
   previously just logging results.
8. Implement DLQ publishing: on Ollama failure exhaustion (from Phase 7's
   retry logic) OR callback failure exhaustion, publish to incident.ai.dlq
   with the exact payload shape from Section 9.4 (eventId, originalTopic,
   incidentId, errorMessage, retryCount, failedAt), and do NOT crash the
   consumer or block the partition — move on to the next message.
9. Implement global exception handlers (@app.exception_handler) for
   OllamaTimeoutError, ValidationError, and generic Exception, per
   Section 24.2, ensuring all failure paths terminate cleanly (log + DLQ),
   never an unhandled crash that kills the consumer process.
10. Write an end-to-end test (Testcontainers or docker-compose-based):
    publish an incident.created event, assert that within a bounded time
    the corresponding incident in Spring Boot has aiProcessed=true with
    correct category/priority/resolutionSuggestion, and that
    incident.ai.completed was published.

ACCEPTANCE CRITERIA:
- Full loop works: incident created -> Kafka -> FastAPI classifies ->
  callback -> Spring Boot persists/re-indexes/invalidates/publishes
  completion -> incident visible via GET /incidents/{id} with AI fields
  populated.
- A simulated Spring Boot outage during callback triggers FastAPI's circuit
  breaker correctly rather than cascading failures.
- A simulated Ollama or ES outage results in a correctly-shaped
  incident.ai.dlq message and the incident remains aiProcessed=false rather
  than silently failing.

If the exact mechanism for "manual or scheduled reprocessing" of
aiProcessed=false incidents (mentioned in Section 15.1 step 9) should be
built now as a real scheduled job, or just left as a documented future
capability for this phase, ask me — it affects whether this phase needs a
reprocessing endpoint/job or not.
```

---

## Phase 10 — Frontend (React + Vite)

**Depends on:** Phases 2, 3, 5, 6–9 (needs a working API surface to call)
**SDD Sections:** 2 (frontend folder structure), 6.1 (all consumed endpoints), 7.2 (response DTOs), 13–14 (auth flow, client side), 26 (React coding standards)
**Note:** The SDD has no wireframes — this prompt includes a lightweight design direction since none exists yet (see the design brief embedded below). Confirm you're happy with this direction before Claude Code builds it, or adjust the brief first.

### Prompt

```
This is Phase 10 of the IncidentIQ build: the React frontend. The backend
API surface (Phases 2-9) is complete and live. There is no existing
wireframe or design system for this product — you are responsible for both
the UI/UX design direction and the implementation. Build something that
feels like a real, polished incident-management product, not a generic CRUD
admin panel.

DESIGN BRIEF (no SDD section covers this — follow this brief directly):
- Aesthetic reference points: lean toward the clean, dense, keyboard-friendly
  feel of tools like Linear, Vercel's dashboard, or PagerDuty's incident
  view — NOT a bootstrap-admin-template look. Generous whitespace, a tight
  type scale, subtle borders over heavy shadows, restrained color used
  purposefully (status/priority badges should be the main color signal in
  an otherwise neutral UI).
- Use Tailwind CSS as the styling foundation. Use shadcn/ui-style
  components (button, input, dialog, dropdown, table, badge, toast,
  skeleton loaders) for consistency and accessibility — build a small local
  component library under src/components/ui/ rather than pulling in a heavy
  full UI kit.
- Support both light and dark mode (a simple theme toggle is enough; default
  to system preference).
- Fully responsive: usable down to mobile width (incident list collapses to
  cards, detail view stacks vertically), not just "doesn't break."
- Priority/status should be color-coded consistently everywhere they
  appear (e.g., P1 = red, P2 = orange, P3 = yellow, P4 = neutral;
  OPEN/IN_PROGRESS/RESOLVED/CLOSED each get a distinct, consistent badge
  color) — define this as a shared constant, not ad hoc per component.
- Favor optimistic UI and skeleton loading states over spinners-everywhere;
  the incident-creation and resolve flows especially should feel instant.
- Interactivity should include at least: sortable/filterable incident table,
  a command-palette-style quick search (even a simple Cmd+K modal hitting
  the keyword search endpoint is enough — doesn't need to be exhaustive),
  toast notifications for actions, and a live-feeling "AI processing..."
  state on newly created incidents that resolves into populated AI fields
  once the backend callback completes (poll every few seconds, per
  Section 1.3 step 7 of the SDD — no websockets required for MVP1).

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 2 — exact frontend folder structure (src/api, components,
  features/{incidents,auth,dashboard}, hooks, store, routes)
- Section 6.1 — every endpoint you'll be calling: auth, incidents CRUD,
  search, runbook search, comments, history
- Section 7.2 — exact response shapes (IncidentResponse, UserSummary,
  AuthResponse, PageResponse<T>)
- Section 13-14 — client-side auth handling: access token in memory (NOT
  localStorage), refresh token handled via HttpOnly cookie automatically by
  the browser, silent refresh on 401, redirect to login on refresh failure
- Section 14.3 — RBAC: hide/disable actions the current user's role
  shouldn't be able to perform (VIEWER can't see create/resolve/delete
  controls, etc.) — this is a UX nicety, the backend is still the real
  enforcement point
- Section 26 (React/TypeScript standards) — strict TS, no implicit any,
  functional components + hooks only, React Query for server state,
  Zustand for light client/UI state, no Redux, ESLint + Prettier

TASKS:
1. Set up Tailwind, a small shadcn/ui-style component library, dark mode
   toggle, and the routing skeleton (routes/) per Section 2's structure.
2. Build features/auth/: login and register pages, access-token-in-memory
   handling via a Zustand store, axios/fetch interceptor doing silent
   refresh on 401 and redirecting to /login on refresh failure.
3. Build features/incidents/: 
   - Incident list view: sortable/filterable table (desktop) collapsing to
     cards (mobile), status/priority badges, pagination wired to
     GET /api/v1/incidents.
   - Incident detail view: full incident info, AI fields (with a clear
     "AI is still processing..." skeleton/badge state when aiProcessed is
     false, polling until it flips true), comments thread, audit history
     timeline, resolve action (role-gated).
   - Create incident form/modal with validation matching Section 22's
     rules client-side (mirroring, not replacing, server validation).
   - Cmd+K quick search modal hitting GET /api/v1/incidents/search.
4. Build features/dashboard/: an overview page using the dashboard
   aggregates from Phase 4 (open/P1 counts) — simple, clean stat cards, not
   an overwrought chart-heavy dashboard.
5. Wire React Query for all server state (incidents, comments, history,
   search results) with sensible cache/staleTime choices, and Zustand only
   for ephemeral UI state (theme, modal open/closed, current user session).
6. Implement toast notifications for create/update/resolve/comment success
   and error states, using the standard error envelope from Section 21.1 to
   surface meaningful messages (not raw stack traces).
7. Ensure full keyboard accessibility and responsive behavior down to
   ~375px width.

ACCEPTANCE CRITERIA:
- A user can register, log in, see the incident list, create an incident,
  watch it transition from "AI processing" to fully classified without a
  manual refresh, comment on it, view its history, and resolve it — all
  with no console errors and no layout breakage at mobile width.
- VIEWER-role users see a read-only experience (creation/resolve/delete
  controls hidden or disabled).
- Dark mode and light mode both render correctly across every screen.

If you want a different visual direction than the brief above (e.g., a
specific existing design system, a particular color palette, or a different
reference product), tell me before Claude Code starts building — this brief
was generated in the absence of any wireframe, so it's a starting point, not
a fixed requirement.
```

---

## Phase 11 — Cross-Cutting Polish (Logging, Naming, Swagger)

**Depends on:** All prior phases
**SDD Sections:** 23 (Logging), 27 (Package Naming), 31 (Swagger/OpenAPI)
**Goal:** A final consistency pass — distributed tracing, OpenAPI documentation completeness, and naming-convention audit — across both backend services.

### Prompt

```
This is Phase 11, the final cross-cutting polish pass across the IncidentIQ
backend services. All functional phases (0-9) are complete. This phase does
NOT add new features — it hardens observability and documentation on what
already exists.

CONTEXT:
Refer to IncidentIQ_SDD.md, specifically:
- Section 23 (Logging Strategy) — structured JSON logs, traceId
  propagation via MDC (Spring Boot) and structlog (FastAPI), the traceId
  must flow through Kafka event payloads so a single request can be
  followed across Spring Boot -> Kafka -> FastAPI -> Ollama -> callback.
  Log level guidance (ERROR/WARN/INFO/DEBUG) and PII handling (never log
  passwords/tokens, mask emails in non-debug logs).
- Section 27 (Package Naming) — confirm both services match the documented
  package/namespace conventions exactly.
- Section 31 (Swagger/OpenAPI) — springdoc-openapi for Spring Boot, native
  FastAPI /docs (internal-only), tag grouping (31.1), per-endpoint
  documentation requirements (31.2), bearerAuth security scheme (31.3),
  /internal/** excluded from the public spec.

TASKS:
1. Add an X-Trace-Id propagation mechanism: Spring Boot generates one per
   inbound request if not present (via a filter, stored in MDC for
   Logback's LogstashEncoder), includes it in every Kafka event payload
   published (add traceId to the event schemas if not already present from
   earlier phases — note this is an addition beyond the literal payload
   shapes in Sections 9.1-9.3, so flag this as an intentional, additive
   change rather than a silent schema deviation), and FastAPI extracts it
   from consumed events, threading it through structlog context for every
   log line including Ollama call latency logs.
2. Audit log levels across both services against Section 23's table —
   ensure unhandled exceptions/downstream failures are ERROR, retries/cache
   misses/validation failures are WARN, lifecycle events (created/updated/
   resolved/login/logout) are INFO, and verbose query/cache-key details are
   DEBUG (disabled in prod profile).
3. Confirm PII handling: no full request/response bodies with
   passwords/tokens are ever logged; emails are masked
   (e.g., j***@domain.com) in non-debug logs.
4. Add the sampled-logging flag for AI prompt/response content (1% sample
   rate, configurable) if Phase 7/8 left this as a TODO.
5. Complete Swagger/OpenAPI annotations across all Spring Boot controllers:
   @Operation summaries/descriptions, @Schema constraints on DTOs, explicit
   @ApiResponse entries for every status code in Section 21.2 relevant to
   that endpoint, bearerAuth security scheme applied globally except the
   public auth endpoints, tag grouping exactly per Section 31.1, and
   springdoc.paths-to-exclude: /internal/** confirmed working.
6. Audit naming conventions across both services against Section 25's
   table — Java classes/methods, REST paths, JSON fields, DB columns,
   Kafka topics, Python modules/classes, Redis keys, env vars — fix any
   drift found.
7. Generate a final docs/api-contracts/ directory with the JSON Schema
   definitions for all four Kafka event payloads (Section 9), referenced
   by Section 9's "Schema governance" note, if not already created in
   earlier phases.

ACCEPTANCE CRITERIA:
- A single user-facing request (e.g., create incident) can be traced
  end-to-end through logs across both services using one traceId.
- /swagger-ui.html shows a complete, accurate, correctly-tagged public API
  surface with no /internal/** paths visible.
- No password, token, or unmasked-PII values appear in any log output at
  INFO level or above.
- A naming-convention audit finds no violations against Section 25.

If adding traceId to the Kafka event payloads counts as a breaking schema
change you'd rather handle via the SDD's "new topic or version bump with
dual-publish" rule (Section 9's schema governance note) instead of an
in-place additive field, tell me which approach you want before this phase
touches the event schemas.
```

---

## Quick Reference — Phase Dependency Chain

```
Phase 0  (scaffolding)
   │
Phase 1  (schema/entities)
   │
Phase 2  (auth/JWT)
   │
Phase 3  (incident CRUD)
   ├── Phase 4  (Redis caching)
   ├── Phase 5  (ES keyword search)
   └── Phase 6  (Kafka producer)
              │
         Phase 7  (FastAPI categorize/prioritize)
              │
         Phase 8  (resolution suggestion + seed data)
              │
         Phase 9  (callback loop, closes the cycle)
   │
Phase 10 (frontend — needs 2,3,5,6-9 live)
   │
Phase 11 (cross-cutting polish — needs everything)
```

Phases 4, 5, and 6 can technically run in parallel (all depend only on
Phase 3) if you're comfortable juggling multiple Claude Code sessions, but
running them sequentially is simpler to verify.

---

*End of Playbook — pair with IncidentIQ_SDD.md v1.1 (MVP1 Scope)*