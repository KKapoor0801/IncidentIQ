# Memory Leak / OOM Kill Loops

## Symptoms
- Pods being OOM-killed repeatedly (visible in `kubectl get events`)
- Application heap usage growing linearly over time without leveling off
- Service restarts every few hours with exit code 137

## Remediation Steps
1. Check pod events: `kubectl describe pod <pod-name>` — look for OOMKilled reason
2. Capture a heap dump before the next OOM: `jmap -dump:live,format=b,file=heap.hprof <pid>`
3. Analyze with Eclipse MAT or VisualVM to identify the leaking object graph
4. As immediate mitigation, increase memory limits in the deployment spec and restart
5. If the leak is in a third-party library, check for known issues and upgrade if a fix exists
