# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# IncidentIQ

AI-powered Incident Intelligence Platform. Polyglot, event-driven, four-tier system.

**Stack:** React + Vite (TS) → Spring Boot 4 (Java 21) → Kafka → Python FastAPI → Ollama.
Data: PostgreSQL, Redis, Elasticsearch.

**Docs:**
- Full design contract: `docs/IncidentIQ_SDD.md` — binding spec, do not diverge without flagging it.
- Build phases: `docs/ClaudeCode_PromptPlaybook.md` — implement one phase per session.
- Decisions log: `docs/decisions.md` — check before asking; update after resolving any
  "ask me" checkpoint from a phase prompt.

**Scope guardrail (MVP1):** No embeddings, no vector DB, no LangChain, no RAG framework,
no Confluence/GitHub integrations, no document ingestion pipelines. AI tier =
categorization + prioritization + keyword-matched resolution suggestion only, via direct
Ollama HTTP calls. If a task seems to need semantic/vector search, stop and flag it —
that's MVP2, deferred (see SDD Appendix C).

**Conventions:**
- Java: constructor injection only, no field `@Autowired`. Services return DTOs, never
  JPA entities, across controller boundaries. No business logic in controllers.
- Python: type hints mandatory (mypy strict), Pydantic models for all schemas — no raw
  dicts crossing service boundaries. Async I/O for Kafka/HTTP handlers.
- React: strict TypeScript, functional components + hooks only. React Query for server
  state, Zustand for light UI state only — no Redux.
- Naming: see SDD Section 25 (Java UpperCamelCase, Python snake_case, Kafka topics
  dot.separated, Redis keys colon-delimited, DB columns snake_case).
- Internal endpoints (`/internal/**`) are never exposed publicly or via JWT auth — separate
  service-token filter chain only.

**Working agreement:**
- Each phase prompt defines its own scope and explicit non-goals — stay inside them; later
  phases build on earlier ones.
- If a phase prompt has an "ask me before assuming" checkpoint, stop and ask — don't guess
  and don't silently pick a default.
- Don't add functionality, dependencies, or abstractions beyond what the current phase asks
  for, even if it seems like a natural extension.

**Build & Run Commands:**
- Full stack: `docker-compose -f infra/docker-compose.yml up --build`
- Spring Boot (dev): `cd backend-core && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Spring Boot tests: `cd backend-core && ./mvnw test`
- Single test: `cd backend-core && ./mvnw test -Dtest=ClassName#methodName`
- Frontend (dev): `cd frontend && npm run dev`
- Frontend lint: `cd frontend && npm run lint`
- FastAPI (dev): `cd ai-service && uvicorn app.main:app --reload --port 8000`
- Python lint: `cd ai-service && ruff check . && mypy --strict app/`

**Project layout:**
- `backend-core/` — Spring Boot 4 core service (`com.incidentiq.core`), Maven build
- `ai-service/` — Python FastAPI AI service
- `frontend/` — React + Vite + TypeScript SPA
- `infra/` — docker-compose, ES mappings, Kafka config
- `docs/` — SDD (binding spec), prompt playbook, decisions log

**Current status:** Phase 0 complete (repo scaffolded, all services bootable, docker-compose ready).