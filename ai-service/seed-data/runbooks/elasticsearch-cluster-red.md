# Elasticsearch Cluster Red Status

## Symptoms
- Cluster health reporting RED in monitoring dashboards
- Search queries returning partial or no results
- Unassigned primary shards visible in `_cluster/health`

## Remediation Steps
1. Check cluster health: `curl localhost:9200/_cluster/health?pretty`
2. Identify unassigned shards: `curl localhost:9200/_cat/shards?v&h=index,shard,prirep,state,unassigned.reason | grep UNASSIGNED`
3. If disk space is the issue, free space or add nodes; check with `curl localhost:9200/_cat/allocation?v`
4. Force shard allocation if safe: `curl -X POST localhost:9200/_cluster/reroute?retry_failed=true`
5. If a node crashed, wait for it to rejoin; if hardware failure, replace the node and let ES rebalance
