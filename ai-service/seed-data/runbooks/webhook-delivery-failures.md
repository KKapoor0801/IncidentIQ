# Third-Party Webhook Delivery Failures

## Symptoms
- Partner/customer webhook endpoints returning non-2xx responses
- Retry queue growing with undelivered webhook payloads
- External integrations reporting missing event notifications

## Remediation Steps
1. Check webhook delivery logs for error patterns — group by endpoint and HTTP status
2. For 5xx responses: the receiver is having issues; implement exponential backoff and notify the partner
3. For 4xx responses: payload format may have changed; check recent API changes against webhook schema
4. Verify webhook signing — expired or rotated signing keys will cause 401/403 rejections
5. If delivery queue is large, consider pausing non-critical webhooks to reduce pressure on the retry system
