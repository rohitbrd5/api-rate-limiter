# API Rate Limiter

A thread-safe, per-client API rate limiting microservice built with Spring Boot 3.2. Uses the **Token Bucket** algorithm with configurable limits per client, supporting per-second, per-minute, and per-hour windows.

## Overview

This service provides a centralized, in-memory rate limiter for backend APIs. Each client (identified by a `clientId`) gets its own configurable rate limit. Requests exceeding the limit are rejected with HTTP `429 Too Many Requests`.

---

## How It Works


Tokens refill lazily on each request (not on a timer), keeping the implementation simple and memory-efficient.

**Example:**
- Limit: 10 requests/second
- Client makes 10 requests at t=0 → all allowed, bucket empty
- At t=0.5s → 5 tokens refilled, 5 more requests allowed
- At t=1.0s → bucket full again (10 tokens)

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Web** (REST endpoints)
- **Spring Boot Actuator** (health checks, metrics)
- **Lombok** (boilerplate reduction)
- **Springdoc OpenAPI 2.2.0** (Swagger UI)
- **JUnit 5** (testing)
- **Maven** (build tool)

---


Verify your setup:

```bash
java -version
mvn -version
```

---

## How to Run

### Option 1: Run with Maven

```bash
# Clone the repository
git clone <repository-url>
cd api-rate-limit

# Run the application
mvn spring-boot:run
```

The service will start on **http://localhost:8080**

### Option 2: Build and Run JAR

```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/api-rate-limiter-1.0.0.jar
```

### Option 3: Run from IDE

1. Open the project in IntelliJ IDEA / Eclipse / VS Code
2. Run `RateLimiterApplication.java` as a Java application

### Verify It's Running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-rate-limiter

# Rate Limiting Configuration
rate-limit:
  # Default limits for unknown clients
  default-limit: 60
  default-window: seconds

  # Per-client rate limit configurations
  clients:
    customerA:
      limit: 100
      window: minutes      # 100 requests/minute
    customerB:
      limit: 1000
      window: minutes      # 1000 requests/minute
    customerC:
      limit: 10
      window: seconds      # 10 requests/second

# Spring Boot Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info

# Swagger/OpenAPI
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```


---

## API Endpoints

### 1. Check if Request is Allowed

**`GET /api/ratelimit/{clientId}/check`**

Check if a request from the given client is allowed under the rate limit.

**Response:**
- `200 OK` — Request allowed
- `429 Too Many Requests` — Rate limit exceeded

**Response body:**
```json
{
  "allowed": true,
  "clientId": "customerA"
}
```

### 2. Get Current Status

**`GET /api/ratelimit/{clientId}/status`**

Get current usage statistics for a client.

**Response:**
```json
{
  "clientId": "customerA",
  "limit": 100,
  "remaining": 95,
  "resetInSeconds": 60
}
```

### 3. Health Check

**`GET /actuator/health`**

Spring Boot Actuator health endpoint.

**Response:**
```json
{
  "status": "UP"
}
```

### 4. Swagger UI

**`GET /swagger-ui.html`**

Interactive API documentation [Swagger UI Dashboard](http://localhost:8080/swagger-ui.html)

---

## Examples

### Example 1: Check rate limit for `customerA` (100 req/min)

```bash
curl -i http://localhost:8080/api/ratelimit/customerA/check
```

**Output:**
```
HTTP/1.1 200 OK
Content-Type: application/json

{"allowed":true,"clientId":"customerA"}
```

### Example 2: Exhaust the limit for `customerC` (10 req/sec)

```bash
# Make 10 requests (all should succeed)
for i in {1..10}; do
  curl -s http://localhost:8080/api/ratelimit/customerC/check
  echo ""
done

# 11th request should be rate-limited
curl -i http://localhost:8080/api/ratelimit/customerC/check
```

**Output:**
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"allowed":false,"clientId":"customerC"}
```

### Example 3: Check status for unknown client (uses default limit)

```bash
curl http://localhost:8080/api/ratelimit/newclient/status
```

**Output:**
```json
{
  "clientId": "newclient",
  "limit": 0,
  "remaining": 0,
  "resetInSeconds": 0
}
```

### Example 4: Check status after making requests

```bash
# Make 3 requests
curl http://localhost:8080/api/ratelimit/customerA/check
curl http://localhost:8080/api/ratelimit/customerA/check
curl http://localhost:8080/api/ratelimit/customerA/check

# Check status
curl http://localhost:8080/api/ratelimit/customerA/status
```

**Output:**
```json
{
  "clientId": "customerA",
  "limit": 100,
  "remaining": 97,
  "resetInSeconds": 60
}
```

---

## Build

### Clean Build

```bash
mvn clean package
```

This produces `target/api-rate-limiter-1.0.0.jar`

### Skip Tests

```bash
mvn clean package -DskipTests
```

### Run with Custom Port

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

Or:

```bash
java -jar target/api-rate-limiter-1.0.0.jar --server.port=9090
```

---

- **Inactive Client Cleanup**: Automatically runs a scheduled task to sweep and remove inactive client rate limiters from memory. 
- **Configurable Intervals**: Manage execution frequency and idle timeouts using the `rate-.cleanup.*` properties.
```



## Future Enhancements

- **Redis Backend** — Distributed rate limiting for multi-instance deployments

---

