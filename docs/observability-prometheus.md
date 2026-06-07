# Prometheus Metrics

Application metrics are exposed at:

```text
GET /actuator/prometheus
```

Prometheus scrape example:

```yaml
scrape_configs:
  - job_name: won-card-channel-server
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - host.docker.internal:8081
```

## API Requests

Spring Boot exposes server-side HTTP metrics as `http_server_requests_seconds_*`.

```promql
# RPS by endpoint
sum(rate(http_server_requests_seconds_count{application="won-card-channel-server"}[1m])) by (uri, method)

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))

# p95 latency
histogram_quantile(
  0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri, method)
)

# Status code ratio
sum(rate(http_server_requests_seconds_count[5m])) by (status)
/
sum(rate(http_server_requests_seconds_count[5m]))
```

## JVM

```promql
jvm_memory_used_bytes{area="heap"}
jvm_memory_used_bytes{area="nonheap"}
rate(jvm_gc_pause_seconds_count[5m])
rate(jvm_gc_pause_seconds_sum[5m])
jvm_threads_live_threads
jvm_threads_deadlocked_threads
jvm_classes_loaded_classes
```

## Thread Pools

Embedded Tomcat request threads:

```promql
tomcat_threads_busy_threads
tomcat_threads_current_threads
tomcat_threads_config_max_threads
tomcat_threads_queue_size
```

Custom sweep result consumer executor:

```promql
app_executor_threads_active{name="sweep-result-consumer"}
app_executor_threads_current{name="sweep-result-consumer"}
app_executor_threads_max{name="sweep-result-consumer"}
app_executor_queue_size{name="sweep-result-consumer"}
```

## DB Connection Pools

Main Spring datasource is exposed by Spring Boot/Hikari:

```promql
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_max
hikaricp_connections_pending
hikaricp_connections_acquire_seconds_count
hikaricp_connections_acquire_seconds_sum
hikaricp_connections_timeout_total
```

AI query datasources are exposed as custom gauges:

```promql
app_datasource_connections_active{pool="card-ai"}
app_datasource_connections_idle{pool="card-ai"}
app_datasource_connections_max{pool="card-ai"}
app_datasource_connections_pending{pool="card-ai"}
app_datasource_connections_active{pool="securities-ai"}
```

## External API Calls

OpenFeign Micrometer metrics are enabled. Depending on Spring Cloud/OpenFeign internals, check one of these names in `/actuator/metrics`:

```text
http.client.requests
feign.client.requests
```

PromQL examples:

```promql
# External call rate
sum(rate(http_client_requests_seconds_count[1m])) by (client_name, method, uri)

# External call failures
sum(rate(http_client_requests_seconds_count{status=~"4..|5..|IO_ERROR"}[5m])) by (client_name)

# External p95 latency
histogram_quantile(
  0.95,
  sum(rate(http_client_requests_seconds_bucket[5m])) by (le, client_name, method, uri)
)
```

Circuit breaker metrics are emitted when Feign circuit breakers are created and used:

```promql
resilience4j_circuitbreaker_state
resilience4j_circuitbreaker_calls_seconds_count
resilience4j_circuitbreaker_failure_rate
resilience4j_circuitbreaker_slow_call_rate
```

Retry metrics require actual Resilience4j retry usage, for example `@Retry` on a service method or a retry-enabled decorator:

```promql
resilience4j_retry_calls_total
```

