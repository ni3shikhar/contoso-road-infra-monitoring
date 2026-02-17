# Integration Tests

Comprehensive end-to-end and RBAC integration tests for the Road Infrastructure Monitoring System.

## Overview

This module contains integration tests that verify:

1. **End-to-End Flow** (`EndToEndFlowIT.java`)
   - Complete sensor registration to alert generation flow
   - Health score calculations
   - KPI updates
   - Analytics data availability

2. **RBAC Permission Enforcement** (`RbacIntegrationIT.java`)
   - All 9 persona role permissions
   - Account lockout after failed logins
   - Token expiry and refresh
   - Cross-role permission matrix

## Test Users

| Role | Username | Password | Permissions |
|------|----------|----------|-------------|
| ADMIN | admin | Admin@123 | Full system access |
| MANAGER | manager | Manager@123 | Strategic oversight, read all |
| SUPERVISOR | supervisor | Supervisor@123 | Operational oversight |
| ENGINEER | engineer | Engineer@123 | Create/update assets, sensors |
| ANALYST | analyst | Analyst@123 | Read-only data and reports |
| TECHNICIAN | technician | Technician@123 | Update sensor status, readings |
| OPERATOR | operator | Operator@123 | Acknowledge alerts, progress |
| AUDITOR | auditor | Auditor@123 | Read-only audit logs |
| VIEWER | viewer | Viewer@123 | Read-only basic access |

## Running Tests

### Prerequisites

1. All services must be running (via docker-compose)
2. API Gateway must be accessible at `http://localhost:8080`
3. Test users must exist in the auth database

### Run All Integration Tests

```bash
cd integration-tests
mvn failsafe:integration-test -Pintegration-tests
```

### Run Specific Test Class

```bash
# End-to-end flow tests
mvn failsafe:integration-test -Pintegration-tests -Dit.test=EndToEndFlowIT

# RBAC tests
mvn failsafe:integration-test -Pintegration-tests -Dit.test=RbacIntegrationIT
```

### Run with Custom API Gateway URL

```bash
mvn failsafe:integration-test -Dapi.gateway.url=http://custom-url:8080
```

### Run in Docker Environment

```bash
mvn failsafe:integration-test -Pdocker
```

## Test Structure

### EndToEndFlowIT

Tests are ordered to follow the complete system flow:

| Order | Test | Description |
|-------|------|-------------|
| 1-2 | Asset Management | Create and retrieve assets |
| 10-12 | Sensor Registration | Register, retrieve, activate sensors |
| 20-22 | Reading Submission | Submit normal and batch readings |
| 30-33 | Alert Generation | Submit anomaly, verify alert, acknowledge, resolve |
| 40-42 | Health Scores | Verify asset and sensor health scores |
| 50-51 | Analytics | Retrieve trends and summaries |
| 60-62 | Sensor Lifecycle | Maintenance mode, calibration, reactivation |
| 70-72 | Filtering | Filter and search operations |
| 80 | Audit Trail | Verify audit logs |
| 90-92 | Error Handling | 404, 400, 401 responses |

### RbacIntegrationIT

Tests verify permission enforcement for each role:

| Order | Role | Tests |
|-------|------|-------|
| 100-104 | ADMIN | Create users, assets, delete, audit access, unlock |
| 200-203 | ENGINEER | Create sensors, update, cannot delete, cannot create users |
| 300-303 | OPERATOR | Acknowledge alerts, read, cannot create/delete |
| 400-405 | VIEWER | Read only, POST/PUT/DELETE return 403 |
| 500-502 | ANALYST | Read analytics, trends, cannot create |
| 600-602 | TECHNICIAN | Update status, submit readings, cannot delete |
| 700-702 | AUDITOR | Read audit logs, read assets, cannot modify |
| 800-801 | SUPERVISOR | Read all assets and alerts |
| 900-901 | MANAGER | Read KPIs and all resources |
| 1000 | Security | Account lockout test |
| 1100-1101 | Security | Token expiry and refresh |
| 1200 | Matrix | Permission matrix validation |
| 1300 | Auth | All roles can authenticate |

## Configuration

### application.properties

```properties
# Default API Gateway URL
api.gateway.url=http://localhost:8080
```

### Maven Profiles

- `integration-tests`: Local development (localhost:8080)
- `docker`: Docker environment (api-gateway:8080)

## Dependencies

- JUnit 5
- REST Assured 5.4.0
- Awaitility 4.2.0
- AssertJ
- Testcontainers (optional)
- Jackson
- Logback

## Test Reports

Reports are generated in `target/failsafe-reports/`:

- `failsafe-summary.xml` - Summary of test results
- `TEST-*.xml` - Individual test class reports

## Troubleshooting

### Services Not Ready

Tests use Awaitility to wait for services:
```java
await()
    .atMost(Duration.ofMinutes(5))
    .pollInterval(Duration.ofSeconds(5))
    .until(this::isApiGatewayHealthy);
```

### Authentication Failures

- Verify test users exist in auth_db
- Check JWT secret matches across services
- Clear token cache with `clearTokenCache(username)`

### Timeout Issues

Increase wait times in test assertions:
```java
await()
    .atMost(Duration.ofSeconds(60)) // Increase timeout
    .pollInterval(Duration.ofSeconds(2))
    .untilAsserted(() -> { ... });
```

## Best Practices

1. **Isolation**: Each test class manages its own test data
2. **Cleanup**: `@AfterAll` methods delete test resources
3. **Ordering**: Tests are ordered to build on previous results
4. **Retries**: Use Awaitility for async operations
5. **Logging**: All tests log significant actions and results
