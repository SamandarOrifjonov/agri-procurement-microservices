# Configuration Guide - Agrifood Platform

## 📋 Overview

Har bir service uchun production-ready configuration qo'shildi:
- Database connection pooling
- Event publishing (Kafka)
- Saga configuration
- Logging
- Monitoring
- Profile-based configuration (dev/prod)

---

## 🗂️ Configuration Files

```
services/
├── procurement-service/
│   └── src/main/resources/
│       └── application.yml
├── supplier-service/
│   └── src/main/resources/
│       └── application.yml
├── contract-service/
│   └── src/main/resources/
│       ├── application.yml          # Base configuration
│       ├── application-dev.yml      # Development overrides
│       └── application-prod.yml     # Production overrides
└── api-gateway/
    └── src/main/resources/
        └── application.yml
```

---

## 🔧 Key Configuration Sections

### 1. Database Configuration

#### Connection Pooling (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # Max connections
      minimum-idle: 5            # Min idle connections
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
```

**Why?**
- Prevents connection exhaustion
- Improves performance under load
- Handles connection failures gracefully

#### JPA Optimization
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20        # Batch inserts/updates
        order_inserts: true     # Optimize insert order
        order_updates: true     # Optimize update order
```

**Why?**
- Reduces database round trips
- Improves bulk operation performance
- Better for saga compensation (batch deletes)

---

### 2. Event Publishing (Kafka)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all          # Wait for all replicas
      retries: 3         # Retry on failure
    consumer:
      group-id: contract-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

**Why?**
- `acks: all` - Ensures event durability (no data loss)
- `retries: 3` - Handles transient failures
- `group-id` - Enables consumer scaling

**Event Versioning Support:**
```yaml
events:
  version: v1
  backward-compatible: true
```

---

### 3. Saga Configuration (Contract Service)

```yaml
saga:
  # Timeout settings
  step-timeout-seconds: 30           # Max time per step
  compensation-timeout-seconds: 60   # Max time for compensation
  
  # Retry settings
  max-retries: 3                     # Retry failed steps
  retry-delay-ms: 1000               # Initial delay
  retry-multiplier: 2                # Exponential backoff
  
  # Monitoring
  metrics-enabled: true              # Track saga metrics
  alert-on-compensation-failure: true # Alert ops team
  
  # Idempotency
  idempotency-enabled: true          # Prevent duplicate execution
  idempotency-ttl-hours: 24          # Cache saga IDs for 24h
```

**Why?**
- **Timeouts**: Prevent hanging transactions
- **Retries**: Handle transient failures (network, DB)
- **Idempotency**: Safe to retry saga steps
- **Alerts**: Notify ops on compensation failures

**Example: Retry with Exponential Backoff**
```
Attempt 1: Immediate
Attempt 2: Wait 1000ms (1s)
Attempt 3: Wait 2000ms (2s)
Attempt 4: Wait 4000ms (4s)
```

---

### 4. Domain Business Rules

#### Procurement Service
```yaml
domain:
  procurement:
    min-budget: 1000              # Minimum budget (UZS)
    max-budget: 1000000000        # Maximum budget (1B UZS)
    deadline-min-days: 7          # Min 7 days deadline
    deadline-max-days: 365        # Max 1 year deadline
```

#### Supplier Service
```yaml
domain:
  supplier:
    min-rating: 0.0               # Minimum rating
    max-rating: 5.0               # Maximum rating
    default-rating: 0.0           # New supplier rating
    verification-required: true   # Require verification
```

#### Contract Service
```yaml
domain:
  contract:
    min-amount: 1000              # Minimum contract amount
    max-amount: 1000000000        # Maximum contract amount
    auto-sign-enabled: false      # Require manual signing
```

**Why?**
- Centralized business rules
- Easy to change without code modification
- Environment-specific rules (dev vs prod)

---

### 5. External Service URLs (for Saga)

```yaml
external-services:
  supplier-service:
    url: http://localhost:8085
    timeout-ms: 5000              # 5 second timeout
  procurement-service:
    url: http://localhost:8084
    timeout-ms: 5000
  email-service:
    url: http://localhost:8087
    timeout-ms: 10000             # Longer for email
    enabled: true                 # Can disable in dev
```

**Why?**
- Saga steps call external services
- Configurable timeouts per service
- Can disable services in dev (e.g., email)

**Used in Saga Steps:**
```java
// ReserveSupplierCapacityStep
String url = externalServices.getSupplierServiceUrl();
int timeout = externalServices.getSupplierServiceTimeout();
restTemplate.postForObject(url + "/reserve", request, Response.class);
```

---

### 6. Logging Configuration

#### Development
```yaml
logging:
  level:
    root: INFO
    com.agrifood.contract: DEBUG        # Verbose for debugging
    com.agrifood.common.saga: DEBUG     # Saga execution logs
    org.hibernate.SQL: DEBUG            # Show SQL queries
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Show parameters
```

#### Production
```yaml
logging:
  level:
    root: WARN
    com.agrifood.contract: INFO         # Less verbose
    com.agrifood.common.saga: INFO      # Important saga events only
    org.hibernate.SQL: WARN             # No SQL in production
  file:
    name: /var/log/agrifood/contract-service.log
    max-size: 100MB
    max-history: 30                     # Keep 30 days
```

**Why?**
- Dev: Verbose for debugging
- Prod: Less noise, better performance
- File rotation: Prevent disk full

---

### 7. Monitoring & Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

**Endpoints:**
- `GET /actuator/health` - Service health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus format

**Saga Metrics:**
```
saga_started_total{service="contract"} 1000
saga_completed_total{service="contract"} 950
saga_failed_total{service="contract"} 45
saga_compensated_total{service="contract"} 40
saga_compensation_failed_total{service="contract"} 5
```

---

### 8. API Gateway Configuration

#### Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      contractCircuitBreaker:
        slidingWindowSize: 10              # Track last 10 calls
        minimumNumberOfCalls: 5            # Min calls before opening
        failureRateThreshold: 50           # Open if 50% fail
        waitDurationInOpenState: 5s        # Wait 5s before retry
```

**States:**
```
CLOSED (normal) → OPEN (failing) → HALF_OPEN (testing) → CLOSED
```

#### Retry Configuration
```yaml
filters:
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
      methods: GET,POST
      backoff:
        firstBackoff: 100ms
        maxBackoff: 500ms
        factor: 2
```

**Why?**
- Handles transient failures
- Exponential backoff prevents overwhelming services
- Only retry safe methods (GET, POST)

---

## 🚀 Running with Profiles

### Development
```bash
# Run with dev profile
java -jar contract-service.jar --spring.profiles.active=dev

# Or via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Dev Profile Features:**
- Longer timeouts (easier debugging)
- Verbose logging
- Email disabled
- Auto-create database tables

### Production
```bash
# Run with prod profile
java -jar contract-service.jar --spring.profiles.active=prod

# With environment variables
export DB_HOST=prod-db.example.com
export DB_USERNAME=prod_user
export DB_PASSWORD=secret
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092
java -jar contract-service.jar --spring.profiles.active=prod
```

**Prod Profile Features:**
- Strict timeouts
- Less logging
- Email enabled
- Validate database schema (no auto-create)
- SSL enabled

---

## 🔐 Environment Variables (Production)

### Contract Service
```bash
# Database
export DB_HOST=prod-db.example.com
export DB_PORT=5432
export DB_NAME=contract_db
export DB_USERNAME=contract_user
export DB_PASSWORD=secret123

# Kafka
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092

# External Services
export SUPPLIER_SERVICE_URL=http://supplier-service:8085
export PROCUREMENT_SERVICE_URL=http://procurement-service:8084
export EMAIL_SERVICE_URL=http://email-service:8087

# SSL
export SSL_ENABLED=true
export SSL_KEY_STORE=/etc/ssl/keystore.p12
export SSL_KEY_STORE_PASSWORD=keystorepass
```

---

## 📊 Configuration Comparison

| Feature | Development | Production |
|---------|-------------|------------|
| DB DDL | `update` (auto-create) | `validate` (manual) |
| SQL Logging | `DEBUG` (verbose) | `WARN` (minimal) |
| Saga Timeout | 60s (relaxed) | 30s (strict) |
| Saga Retries | 1 (fast feedback) | 3 (resilient) |
| Email Service | Disabled | Enabled |
| Alerts | Disabled | Enabled |
| Connection Pool | 10 connections | 20 connections |
| Log File | Console only | File + rotation |

---

## 🎯 Best Practices

### 1. Never Hardcode Secrets
```yaml
# ❌ Bad
spring:
  datasource:
    password: mypassword123

# ✅ Good
spring:
  datasource:
    password: ${DB_PASSWORD}
```

### 2. Use Profiles
```yaml
# application.yml (base)
spring:
  application:
    name: contract-service

---
# application-dev.yml (dev overrides)
spring:
  jpa:
    show-sql: true

---
# application-prod.yml (prod overrides)
spring:
  jpa:
    show-sql: false
```

### 3. Configure Timeouts
```yaml
# Prevent hanging requests
saga:
  step-timeout-seconds: 30
external-services:
  supplier-service:
    timeout-ms: 5000
```

### 4. Enable Monitoring
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### 5. Use Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

---

## 🔍 Troubleshooting

### Issue: Saga Timeout
```
Error: Saga step timeout after 30 seconds
```

**Solution:**
```yaml
# Increase timeout in application-dev.yml
saga:
  step-timeout-seconds: 60
```

### Issue: Database Connection Pool Exhausted
```
Error: HikariPool - Connection is not available
```

**Solution:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase pool size
```

### Issue: Kafka Connection Failed
```
Error: Failed to connect to Kafka broker
```

**Solution:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Check Kafka is running
```

---

## 📚 Related Documentation

- [DDD-DOMAIN-REASONING.md](DDD-DOMAIN-REASONING.md) - Domain configuration rationale
- [EVENT-VERSIONING-STRATEGY.md](EVENT-VERSIONING-STRATEGY.md) - Event configuration
- [SAGA-FAILURE-SCENARIOS.md](SAGA-FAILURE-SCENARIOS.md) - Saga configuration usage
- [PRACTICAL-EXAMPLE.md](PRACTICAL-EXAMPLE.md) - Configuration in action

---

## ✅ Summary

Configuration improvements:
- ✅ Database connection pooling
- ✅ Kafka event publishing
- ✅ Saga timeout and retry configuration
- ✅ Domain business rules
- ✅ External service URLs
- ✅ Profile-based configuration (dev/prod)
- ✅ Monitoring and metrics
- ✅ API Gateway resilience (circuit breaker, retry)

All services are now production-ready! 🚀
