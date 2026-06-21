# Kafka Consumer Lag / Backpressure

## Symptoms
- Consumer group lag growing continuously (visible in Kafka monitoring)
- Processing latency increasing for async workflows
- Events being processed minutes or hours after production

## Remediation Steps
1. Check consumer lag: `kafka-consumer-groups --bootstrap-server localhost:9092 --group <group-id> --describe`
2. If lag is due to slow processing, scale consumers up to the partition count (max useful parallelism)
3. Check for poison pill messages causing repeated failures — inspect the DLQ topic
4. Verify consumer commit strategy — if using auto-commit, ensure processing finishes before the commit interval
5. If a burst caused temporary lag, monitor whether consumers are catching up; if so, no action needed
