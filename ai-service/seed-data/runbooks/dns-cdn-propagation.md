# DNS/CDN Propagation Issues

## Symptoms
- Some users reaching old endpoints while others see the new ones
- Intermittent 404s or SSL errors after a DNS change
- CDN cache serving stale content after origin update

## Remediation Steps
1. Check DNS propagation status: `dig +short example.com @8.8.8.8` from multiple regions
2. Verify TTL on the DNS record — if set too high, propagation will be slow; lower it before future changes
3. For CDN stale content: purge the CDN cache via provider dashboard or API
4. If SSL errors after DNS change, ensure the new endpoint has a valid certificate matching the hostname
5. For critical cutover, use weighted DNS routing to gradually shift traffic rather than instant switch
