# Payment Gateway Timeout

## Symptoms
- Checkout requests failing with 500 or 504 errors
- Stripe/payment provider API calls exceeding timeout thresholds
- Spike in failed transactions in payment dashboard

## Remediation Steps
1. Check the payment provider's status page (e.g., status.stripe.com) for ongoing incidents
2. Verify network connectivity from the payment-service pod to the provider endpoint: `curl -v https://api.stripe.com/v1/charges -H "Authorization: Bearer sk_test_..."`
3. Check payment-service connection pool metrics — if exhausted, restart pods: `kubectl rollout restart deployment/payment-service`
4. If provider is healthy, check for recent deployments that may have changed timeout configs
5. As a temporary mitigation, increase the client timeout in payment-service config from 5s to 15s and redeploy
