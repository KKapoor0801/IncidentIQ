# API Rate-Limit Cascading Failure

## Symptoms
- Upstream API returning 429 Too Many Requests
- Retry storms amplifying the load instead of backing off
- Downstream services timing out waiting for the rate-limited API

## Remediation Steps
1. Identify which service is hitting the rate limit: check API gateway logs for 429 responses grouped by client
2. Implement or verify exponential backoff with jitter in the calling service
3. Check if a recent traffic spike or batch job is responsible — pause the batch if so
4. Contact the API provider to request a temporary rate limit increase if legitimate traffic
5. Enable request queuing or circuit breaker pattern to shed excess load gracefully
