# IncidentIQ

AI-powered Incident Intelligence Platform. Automatically categorizes, prioritizes, and suggests resolutions for operational incidents using a local LLM.

## Architecture

| Tier | Technology | Directory |
|---|---|---|
| Presentation | React + Vite + TypeScript | `frontend/` |
| Core Service | Spring Boot 4 (Java 21) | `backend-core/` |
| Intelligence Service | Python FastAPI | `ai-service/` |
| Data/Infra | PostgreSQL, Redis, Elasticsearch, Kafka, Ollama | `infra/` |

## Quick Start (Docker Compose)

```bash
# 1. Copy env file and set secrets
cp infra/.env.example infra/.env
# Edit infra/.env with real values

# 2. Boot the full stack (all 9 containers)
docker-compose -f infra/docker-compose.yml --env-file infra/.env up --build

# 3. Verify
# Spring Boot:    http://localhost:8080/actuator/health
# FastAPI:        http://localhost:8000/health
# Frontend:       http://localhost
# Swagger UI:     http://localhost:8080/swagger-ui.html
# Elasticsearch:  http://localhost:9200
```

## Development (Individual Services)

### Spring Boot Core

```bash
cd backend-core
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Requires: PostgreSQL on :5432, Redis on :6379, Elasticsearch on :9200, Kafka on :9092
# Boot infra only: docker-compose -f infra/docker-compose.yml up incidentiq-postgres incidentiq-redis incidentiq-elasticsearch incidentiq-kafka incidentiq-zookeeper
```

### FastAPI AI Service

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8000
```

### React Frontend

```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173, proxies /api/* to :8080
```

## Project Structure

See `docs/IncidentIQ_SDD.md` for the full design contract. Key directories:

- `backend-core/` — Spring Boot: auth, CRUD, caching, search, Kafka producer
- `ai-service/` — FastAPI: Kafka consumer, Ollama classification, callback
- `frontend/` — React SPA: incident dashboard, auth, search
- `infra/` — Docker Compose, Elasticsearch mappings
- `docs/` — SDD (binding spec), build playbook
