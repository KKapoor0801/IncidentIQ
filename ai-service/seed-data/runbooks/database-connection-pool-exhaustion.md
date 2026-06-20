# Database Connection Pool Exhaustion

## Symptoms
- Application returning 503 errors with "unable to acquire connection" in logs
- HikariCP or PgBouncer metrics showing 0 available connections
- Slow queries blocking the pool, visible in pg_stat_activity

## Remediation Steps
1. Check active connections: `SELECT count(*), state FROM pg_stat_activity GROUP BY state;`
2. Identify long-running queries: `SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC LIMIT 10;`
3. Kill blocking queries if safe: `SELECT pg_terminate_backend(pid);`
4. If pool size is genuinely too small, increase `maximum-pool-size` in HikariCP config (current: 20, safe max: 50 for our Postgres instance)
5. Check for connection leaks — look for unclosed connections in application logs with `grep "connection leak" app.log`
