#!/usr/bin/env python3
"""Seed script for IncidentIQ — idempotent, safe to re-run.

Seeds:
1. Runbooks (Markdown files) → ES `runbooks` index
2. Service metadata (JSON) → ES `service_metadata` index
3. Historical resolved incidents → Postgres `incidents` table + ES `incidents` index
"""

import json
import os
from datetime import UTC, datetime
from pathlib import Path

import httpx
import psycopg2  # type: ignore[import-untyped]

ES_URL = os.environ.get("ES_URIS", "http://localhost:9200")
PG_DSN = os.environ.get(
    "DATABASE_URL",
    "postgresql://incidentiq:incidentiq@localhost:5432/incidentiq",
)

SEED_DIR = Path(__file__).parent.parent / "seed-data"

SEED_INCIDENTS = [
    {
        "id": "00000000-0000-0000-0000-000000000001",
        "title": "Stripe API returning 500 on charge creation",
        "description": "All checkout attempts failing with Stripe API 500 errors since 09:15 UTC. Stripe status page confirms degraded performance.",
        "category": "PAYMENTS",
        "priority": "P1",
        "resolution_notes": "Stripe resolved their API issue at 10:02 UTC. No action needed on our side.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000002",
        "title": "Database connection pool exhausted on order-service",
        "description": "Order-service returning 503 errors. HikariCP shows 0 available connections. pg_stat_activity shows 20 idle-in-transaction connections.",
        "category": "DATABASE",
        "priority": "P1",
        "resolution_notes": "Killed idle-in-transaction sessions and fixed connection leak in OrderRepository.findByStatus() — missing @Transactional causing connections to not release.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000003",
        "title": "Mass token expiry causing auth service CPU spike",
        "description": "Auth service at 95% CPU. Token refresh rate 15x normal. All users being forced to re-authenticate simultaneously.",
        "category": "AUTH",
        "priority": "P1",
        "resolution_notes": "Root cause was a key rotation that invalidated all tokens at once. Added token refresh jitter and increased auth-service replicas to 6.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000004",
        "title": "Kafka consumer lag on orders topic exceeding 100k",
        "description": "Consumer group orders-processor has lag of 120,000 messages. Order confirmations delayed by 45 minutes.",
        "category": "INFRA",
        "priority": "P2",
        "resolution_notes": "Scaled consumer instances from 2 to 6 (matching partition count). Lag cleared within 20 minutes.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000005",
        "title": "Redis cache stampede causing DB load spike",
        "description": "Every 5 minutes, Redis cache for product catalog expires and 500+ concurrent requests hit Postgres simultaneously.",
        "category": "DATABASE",
        "priority": "P2",
        "resolution_notes": "Implemented cache warm-up job that refreshes hot keys 30s before TTL expiry. Added jitter to TTL values (270-330s range).",
    },
    {
        "id": "00000000-0000-0000-0000-000000000006",
        "title": "SSL certificate expired on api.example.com",
        "description": "All HTTPS connections to api.example.com failing. Certificate expired 2 hours ago. Let's Encrypt auto-renewal did not trigger.",
        "category": "NETWORK",
        "priority": "P1",
        "resolution_notes": "Manually renewed certificate with certbot. Fixed cron job path for auto-renewal. Added 14-day expiry alert.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000007",
        "title": "Webhook delivery queue backlog growing",
        "description": "Partner webhook endpoint returning 503. Retry queue has 50,000 undelivered events. Queue growing at 200/min.",
        "category": "INFRA",
        "priority": "P3",
        "resolution_notes": "Partner resolved their server issue after 3 hours. Our retry mechanism cleared the backlog automatically. No data loss.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000008",
        "title": "DNS propagation delay after CDN migration",
        "description": "30% of users still hitting old CDN endpoint 6 hours after DNS change. TTL was set to 86400 (24h).",
        "category": "NETWORK",
        "priority": "P3",
        "resolution_notes": "Waited for TTL to expire. For future changes, pre-lower TTL to 300s 24h before the cutover.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000009",
        "title": "Payment service memory leak causing OOM kills",
        "description": "Payment-service pods OOM-killed every 4 hours. Memory usage grows linearly from 512MB to 2GB limit.",
        "category": "PAYMENTS",
        "priority": "P2",
        "resolution_notes": "Heap dump analysis showed Stripe SDK connection objects not being released. Upgraded stripe-java from 20.x to 22.x which fixed the leak.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000010",
        "title": "Elasticsearch cluster RED — unassigned primary shards",
        "description": "Search returning empty results. ES cluster health RED with 5 unassigned primary shards after node-3 disk filled up.",
        "category": "DATABASE",
        "priority": "P1",
        "resolution_notes": "Freed disk space on node-3 by deleting old indices. Re-allocated shards with _cluster/reroute. Added disk usage alert at 80%.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000011",
        "title": "Rate limiting incorrectly blocking legitimate API traffic",
        "description": "Enterprise customer reporting 429 errors. Rate limit key using IP instead of API key, so all traffic from their NAT gateway shares one bucket.",
        "category": "AUTH",
        "priority": "P2",
        "resolution_notes": "Changed rate limit key from IP to API key for authenticated requests. IP-based limiting kept only for unauthenticated endpoints.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000012",
        "title": "Kubernetes pods in CrashLoopBackOff after config change",
        "description": "All billing-service pods crash-looping. Started after ConfigMap update. Pods exit with code 1 before readiness probe.",
        "category": "INFRA",
        "priority": "P1",
        "resolution_notes": "ConfigMap had a typo in the database URL. Rolled back with kubectl rollout undo. Added config validation in the startup script.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000013",
        "title": "Notification service dropping SMS messages",
        "description": "SMS delivery rate dropped to 60% over the last 24h. Twilio dashboard shows rate limit errors from our account.",
        "category": "INFRA",
        "priority": "P3",
        "resolution_notes": "Hit Twilio's per-second rate limit due to a marketing blast. Implemented queue-based throttling to stay within limits.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000014",
        "title": "Search service returning stale results after reindex",
        "description": "Product search showing items deleted 2 days ago. ES index alias still pointing to the old index after a failed reindex job.",
        "category": "DATABASE",
        "priority": "P3",
        "resolution_notes": "Manually switched the alias to the new index. Fixed the reindex script to swap alias atomically after validation.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000015",
        "title": "API gateway latency spike — P99 from 50ms to 2s",
        "description": "API gateway P99 latency jumped to 2s. All downstream services are healthy. Gateway logs show connection pool exhaustion.",
        "category": "NETWORK",
        "priority": "P2",
        "resolution_notes": "Gateway connection pool to auth-service was sized at 10, insufficient for current traffic. Increased to 50 and added circuit breaker.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000016",
        "title": "Billing cron job double-charging customers",
        "description": "Monthly billing job ran twice due to leader election failure. 200 customers charged double.",
        "category": "PAYMENTS",
        "priority": "P1",
        "resolution_notes": "Added idempotency key to charge creation. Refunded affected customers automatically. Fixed leader election with proper distributed lock.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000017",
        "title": "Inventory service showing negative stock counts",
        "description": "Race condition in stock reservation. Concurrent orders for popular items result in stock going negative.",
        "category": "DATABASE",
        "priority": "P2",
        "resolution_notes": "Replaced optimistic locking with SELECT FOR UPDATE on the stock row. Added a CHECK constraint preventing negative values.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000018",
        "title": "CDN serving HTTP 404 for static assets after deploy",
        "description": "New frontend deploy changed asset hash filenames but CDN still serving old index.html referencing old hashes.",
        "category": "NETWORK",
        "priority": "P2",
        "resolution_notes": "Purged CDN cache for index.html (no-cache header was missing). Added Cache-Control: no-cache for HTML files in CDN config.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000019",
        "title": "Analytics pipeline Kafka deserialization errors",
        "description": "Analytics consumer throwing deserialization exceptions after producer schema change. 40% of events being dropped to DLQ.",
        "category": "INFRA",
        "priority": "P3",
        "resolution_notes": "Producer added a new field without schema registry validation. Added backward-compatible default. Reprocessed DLQ events.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000020",
        "title": "Auth service login endpoint returning 500 intermittently",
        "description": "Login failing for ~5% of requests with NullPointerException in token generation. Stack trace points to missing user role.",
        "category": "AUTH",
        "priority": "P2",
        "resolution_notes": "Users migrated from legacy system had null role column. Added NOT NULL constraint with default VIEWER and backfilled existing rows.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000021",
        "title": "Redis cluster node failure causing partial cache outage",
        "description": "One Redis cluster node unresponsive. Keys on that shard returning CLUSTERDOWN errors. 1/3 of cache reads failing.",
        "category": "DATABASE",
        "priority": "P1",
        "resolution_notes": "Failed node had a hardware issue. Redis Cluster auto-promoted the replica. Replaced the failed node and rebalanced slots.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000022",
        "title": "Order confirmation emails delayed by 3 hours",
        "description": "Email sending backed up. Notification service Kafka consumer lag at 50,000. SES sending rate not the bottleneck.",
        "category": "INFRA",
        "priority": "P3",
        "resolution_notes": "Consumer was doing synchronous DB lookups per email. Switched to batch DB queries (100 at a time). Lag cleared in 30 min.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000023",
        "title": "Payment refund webhook signature validation failing",
        "description": "Stripe refund webhooks being rejected with 401. Started after rotating the webhook signing secret.",
        "category": "PAYMENTS",
        "priority": "P2",
        "resolution_notes": "Old signing secret was still cached in the application. Restarted payment-service to pick up the new secret from Vault.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000024",
        "title": "Load balancer health checks failing after port change",
        "description": "ALB marking all targets unhealthy after service port changed from 8080 to 8081. Health check path still configured for 8080.",
        "category": "INFRA",
        "priority": "P1",
        "resolution_notes": "Updated ALB target group health check port to 8081. Added health check port to the deployment checklist.",
    },
    {
        "id": "00000000-0000-0000-0000-000000000025",
        "title": "Database replication lag causing stale reads",
        "description": "Read replica 45 seconds behind primary. Users seeing old data after writes. Replication lag growing.",
        "category": "DATABASE",
        "priority": "P2",
        "resolution_notes": "Long-running analytics query on the replica was blocking replication. Killed the query and moved analytics to a dedicated replica.",
    },
]


def seed_runbooks(es_url: str) -> int:
    runbooks_dir = SEED_DIR / "runbooks"
    count = 0
    for md_file in sorted(runbooks_dir.glob("*.md")):
        doc_id = f"runbook-{md_file.stem}"
        content = md_file.read_text()
        lines = content.strip().split("\n")
        title = lines[0].lstrip("# ").strip() if lines else md_file.stem

        doc = {
            "id": doc_id,
            "title": title,
            "body": content,
            "tags": [md_file.stem.replace("-", " ")],
            "service": None,
            "lastUpdated": datetime.now(UTC).isoformat(),
        }

        resp = httpx.put(
            f"{es_url}/runbooks/_doc/{doc_id}",
            json=doc,
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()
        count += 1
        print(f"  Indexed runbook: {title}")

    return count


def seed_service_metadata(es_url: str) -> int:
    metadata_file = SEED_DIR / "service_metadata.json"
    services = json.loads(metadata_file.read_text())
    count = 0
    for svc in services:
        doc_id = f"svc-{svc['serviceName']}"
        resp = httpx.put(
            f"{es_url}/service_metadata/_doc/{doc_id}",
            json=svc,
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()
        count += 1
        print(f"  Indexed service: {svc['serviceName']}")

    return count


def seed_incidents(pg_dsn: str, es_url: str) -> int:
    conn = psycopg2.connect(pg_dsn)
    cur = conn.cursor()

    # Ensure a seed user exists for reporter_id
    seed_user_id = "00000000-0000-0000-0000-ffffffffffff"
    cur.execute(
        """
        INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        (seed_user_id, "seed-system@incidentiq.internal",
         "$2a$12$seedhashedpasswordplaceholder", "Seed System", "ENGINEER"),
    )

    count = 0
    for inc in SEED_INCIDENTS:
        inc_id = inc["id"]
        cur.execute(
            """
            INSERT INTO incidents (
                id, title, description, status, priority, category,
                ai_processed, ai_resolution_suggestion, reporter_id,
                created_at, updated_at, resolved_at, version
            ) VALUES (
                %s, %s, %s, 'RESOLVED', %s, %s,
                TRUE, %s, %s,
                NOW() - interval '7 days' * random(),
                NOW() - interval '1 day' * random(),
                NOW() - interval '1 day' * random(),
                0
            )
            ON CONFLICT (id) DO NOTHING
            """,
            (
                inc_id, inc["title"], inc["description"],
                inc["priority"], inc["category"],
                inc["resolution_notes"], seed_user_id,
            ),
        )

        # Also index to ES
        es_doc = {
            "id": inc_id,
            "title": inc["title"],
            "description": inc["description"],
            "status": "RESOLVED",
            "priority": inc["priority"],
            "category": inc["category"],
            "aiResolutionSuggestion": inc["resolution_notes"],
            "reporterId": seed_user_id,
        }
        resp = httpx.put(
            f"{es_url}/incidents/_doc/{inc_id}",
            json=es_doc,
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()
        count += 1

    conn.commit()
    cur.close()
    conn.close()
    print(f"  Seeded {count} historical incidents to Postgres + ES")
    return count


def main() -> None:
    print("=== IncidentIQ Seed Script ===\n")

    print("[1/3] Seeding runbooks to ES...")
    rb_count = seed_runbooks(ES_URL)
    print(f"  Done: {rb_count} runbooks\n")

    print("[2/3] Seeding service metadata to ES...")
    svc_count = seed_service_metadata(ES_URL)
    print(f"  Done: {svc_count} services\n")

    print("[3/3] Seeding historical incidents to Postgres + ES...")
    inc_count = seed_incidents(PG_DSN, ES_URL)
    print(f"  Done: {inc_count} incidents\n")

    # Refresh ES indices
    httpx.post(f"{ES_URL}/runbooks/_refresh")
    httpx.post(f"{ES_URL}/incidents/_refresh")
    httpx.post(f"{ES_URL}/service_metadata/_refresh")

    print(f"Seeding complete: {rb_count} runbooks, {svc_count} services, {inc_count} incidents")


if __name__ == "__main__":
    main()
