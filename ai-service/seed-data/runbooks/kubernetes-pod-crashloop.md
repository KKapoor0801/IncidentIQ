# Kubernetes Pod CrashLoopBackOff

## Symptoms
- Pod status showing CrashLoopBackOff in `kubectl get pods`
- Container restarting every few seconds with increasing backoff delay
- Application logs showing startup failure before becoming ready

## Remediation Steps
1. Check pod logs: `kubectl logs <pod-name> --previous` to see the crash reason
2. Check events: `kubectl describe pod <pod-name>` for resource limits, image pull errors, or probe failures
3. Common causes: missing config/secrets, database not reachable, port conflict, OOM on startup
4. If caused by a bad deployment, rollback: `kubectl rollout undo deployment/<name>`
5. If caused by a dependency not ready, add init containers or readiness gates
