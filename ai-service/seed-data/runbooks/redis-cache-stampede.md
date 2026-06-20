# Redis Cache Stampede

## Symptoms
- Redis CPU spikes to near 100% during cache expiry windows
- Backend database receives sudden burst of identical queries
- Response latency spikes every N minutes aligned with cache TTL

## Remediation Steps
1. Confirm stampede pattern: check Redis `INFO stats` for keyspace miss rate spikes
2. Implement cache warm-up: use a background job to refresh hot keys before TTL expiry
3. Add jitter to TTL values: instead of fixed 300s, use 270-330s random range
4. For critical hot keys, use a mutex/lock pattern — only one request refreshes while others wait
5. Consider using Redis `GETEX` to extend TTL on read (sliding expiration)
