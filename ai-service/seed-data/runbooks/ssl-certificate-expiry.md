# SSL Certificate Expiry

## Symptoms
- Browsers showing "Your connection is not private" errors
- API clients failing with SSL handshake errors
- Certificate expiry warnings in monitoring/alerting system

## Remediation Steps
1. Check certificate expiry: `echo | openssl s_client -connect example.com:443 2>/dev/null | openssl x509 -noout -dates`
2. If using cert-manager in Kubernetes, check the Certificate resource status: `kubectl describe certificate <name>`
3. Manually renew if auto-renewal failed: `certbot renew --force-renewal -d example.com`
4. Verify the renewed cert is deployed: restart the ingress controller or web server
5. Set up monitoring alert for certificates expiring within 14 days to prevent recurrence
