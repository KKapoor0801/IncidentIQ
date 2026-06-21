# Auth/SSO Token Expiry Storm

## Symptoms
- Mass 401 errors across multiple services simultaneously
- Auth service CPU spike from token refresh flood
- Users reporting being logged out across all sessions

## Remediation Steps
1. Check auth-service metrics for token refresh request rate — if 10x normal, confirm it's a token storm
2. Temporarily increase the auth-service replica count: `kubectl scale deployment/auth-service --replicas=6`
3. Check if a recent key rotation invalidated all existing tokens prematurely
4. If caused by clock skew, verify NTP sync across all nodes: `chronyc tracking`
5. Consider enabling token refresh jitter (stagger refresh times by ±30s) to prevent future storms
