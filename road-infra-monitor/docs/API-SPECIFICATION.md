# Road Infrastructure Monitoring - API Specification

## Base URL
All API endpoints are accessible through the API Gateway at:
- Development: `http://localhost:8080/api`
- Production: `https://api.roadinfra.example.com/api`

## Authentication

### Headers
All authenticated endpoints require the following header:
```
Authorization: Bearer <jwt_token>
```

### Token Format
JWT tokens contain:
- `sub`: Username
- `role`: User role (ADMIN, ENGINEER, OPERATOR, VIEWER)
- `permissions`: Array of granted permissions
- `exp`: Expiration timestamp

---

## Auth Service (Port 8086)

### POST /api/auth/login
Authenticate user and obtain JWT tokens.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN",
      "permissions": ["SENSOR_READ", "SENSOR_WRITE", ...]
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### POST /api/auth/register
Register a new user.

**Required Permission:** `USER_MANAGE`

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": "OPERATOR"
}
```

### POST /api/auth/refresh
Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

### POST /api/auth/logout
Invalidate current session.

### GET /api/auth/me
Get current authenticated user.

---

## Sensor Service (Port 8081)

### GET /api/sensors
List all sensors with pagination and filtering.

**Required Permission:** `SENSOR_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number (default: 0) |
| size | integer | Page size (default: 20) |
| type | SensorType | Filter by sensor type |
| assetId | long | Filter by asset |
| status | string | Filter by status (ACTIVE, INACTIVE) |

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Bridge Strain Gauge 01",
        "type": "STRAIN_GAUGE",
        "assetId": 100,
        "assetName": "Highway Bridge A1",
        "location": {
          "latitude": 47.6062,
          "longitude": -122.3321
        },
        "status": "ACTIVE",
        "lastReading": {
          "value": 125.5,
          "unit": "μm/m",
          "timestamp": "2024-01-15T10:30:00Z"
        },
        "installDate": "2023-06-15",
        "calibrationDate": "2024-01-01"
      }
    ],
    "totalElements": 150,
    "totalPages": 8,
    "number": 0
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/sensors/{id}
Get sensor by ID.

**Required Permission:** `SENSOR_READ`

### POST /api/sensors
Create a new sensor.

**Required Permission:** `SENSOR_WRITE`

**Request Body:**
```json
{
  "name": "string",
  "type": "STRAIN_GAUGE",
  "assetId": 100,
  "location": {
    "latitude": 47.6062,
    "longitude": -122.3321
  },
  "unit": "μm/m",
  "minThreshold": 0,
  "maxThreshold": 500,
  "samplingIntervalMs": 1000
}
```

### PUT /api/sensors/{id}
Update an existing sensor.

**Required Permission:** `SENSOR_WRITE`

### DELETE /api/sensors/{id}
Delete a sensor.

**Required Permission:** `SENSOR_DELETE`

### GET /api/sensors/{id}/readings
Get sensor readings with time range.

**Required Permission:** `SENSOR_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| from | datetime | Start of time range |
| to | datetime | End of time range |
| aggregation | string | Aggregation interval (1m, 5m, 1h, 1d) |

### POST /api/sensors/{id}/configure
Configure sensor settings.

**Required Permission:** `SENSOR_CONFIGURE`

---

## Asset Service (Port 8082)

### GET /api/assets
List all assets with pagination.

**Required Permission:** `ASSET_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |
| type | AssetType | Filter by asset type |
| healthStatus | HealthStatus | Filter by health status |
| search | string | Search by name or code |

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 100,
        "code": "BRIDGE-A1-001",
        "name": "Highway Bridge A1",
        "type": "BRIDGE",
        "description": "Main span over River X",
        "location": {
          "type": "LineString",
          "coordinates": [[lng1, lat1], [lng2, lat2]]
        },
        "healthStatus": "HEALTHY",
        "sensorCount": 12,
        "constructionDate": "2010-05-20",
        "lastInspectionDate": "2024-01-10",
        "nextInspectionDate": "2024-07-10"
      }
    ],
    "totalElements": 45,
    "totalPages": 3
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/assets/{id}
Get asset by ID with full details.

**Required Permission:** `ASSET_READ`

### POST /api/assets
Create a new asset.

**Required Permission:** `ASSET_WRITE`

### PUT /api/assets/{id}
Update an existing asset.

**Required Permission:** `ASSET_WRITE`

### DELETE /api/assets/{id}
Delete an asset.

**Required Permission:** `ASSET_DELETE`

### PUT /api/assets/{id}/progress
Update asset progress/status.

**Required Permission:** `ASSET_PROGRESS_UPDATE`

### GET /api/assets/{id}/sensors
Get all sensors attached to an asset.

**Required Permission:** `ASSET_READ`

### GET /api/assets/geojson
Get all assets as GeoJSON FeatureCollection.

**Required Permission:** `ASSET_READ`

---

## Monitoring Service (Port 8083)

### GET /api/monitoring/health
Get system-wide health overview.

**Required Permission:** `MONITORING_READ`

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "overallStatus": "WARNING",
    "summary": {
      "healthy": 38,
      "warning": 5,
      "critical": 2,
      "offline": 0,
      "unknown": 0
    },
    "lastUpdated": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/monitoring/health/{assetId}
Get health status for a specific asset.

**Required Permission:** `MONITORING_READ`

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "assetId": 100,
    "assetName": "Highway Bridge A1",
    "healthStatus": "WARNING",
    "healthScore": 72.5,
    "sensorStatuses": [
      {
        "sensorId": 1,
        "sensorName": "Strain Gauge 01",
        "status": "HEALTHY",
        "value": 125.5,
        "threshold": { "min": 0, "max": 500, "warning": 400 },
        "lastUpdated": "2024-01-15T10:29:55Z"
      }
    ],
    "issues": [
      {
        "type": "THRESHOLD_WARNING",
        "sensorId": 3,
        "message": "Temperature approaching upper threshold"
      }
    ],
    "lastUpdated": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/monitoring/history
Get health history for trending.

**Required Permission:** `MONITORING_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| assetId | long | Filter by asset (optional) |
| from | datetime | Start of time range |
| to | datetime | End of time range |
| interval | string | Aggregation interval |

### POST /api/monitoring/thresholds
Configure monitoring thresholds.

**Required Permission:** `MONITORING_CONFIGURE_THRESHOLDS`

---

## Alert Service (Port 8084)

### GET /api/alerts
List all alerts with filtering.

**Required Permission:** `ALERT_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |
| severity | AlertSeverity | Filter by severity |
| status | string | ACTIVE, ACKNOWLEDGED, RESOLVED |
| assetId | long | Filter by asset |
| from | datetime | Created after |
| to | datetime | Created before |

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 500,
        "title": "Critical strain detected",
        "description": "Strain gauge reading exceeds critical threshold",
        "severity": "CRITICAL",
        "status": "ACTIVE",
        "assetId": 100,
        "assetName": "Highway Bridge A1",
        "sensorId": 1,
        "sensorName": "Strain Gauge 01",
        "triggerValue": 520.5,
        "threshold": 500,
        "createdAt": "2024-01-15T10:28:00Z",
        "acknowledgedAt": null,
        "acknowledgedBy": null,
        "resolvedAt": null,
        "resolvedBy": null,
        "assignedTo": null
      }
    ],
    "totalElements": 23,
    "totalPages": 2
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/alerts/{id}
Get alert by ID.

**Required Permission:** `ALERT_READ`

### GET /api/alerts/active
Get all active (unresolved) alerts.

**Required Permission:** `ALERT_READ`

### PUT /api/alerts/{id}/acknowledge
Acknowledge an alert.

**Required Permission:** `ALERT_ACKNOWLEDGE`

**Request Body:**
```json
{
  "comment": "Investigating the issue"
}
```

### PUT /api/alerts/{id}/assign
Assign alert to a user.

**Required Permission:** `ALERT_ASSIGN`

**Request Body:**
```json
{
  "userId": 5,
  "comment": "Assigned to field engineer"
}
```

### PUT /api/alerts/{id}/resolve
Resolve an alert.

**Required Permission:** `ALERT_RESOLVE`

**Request Body:**
```json
{
  "resolution": "Sensor recalibrated, readings now normal",
  "rootCause": "Sensor drift due to temperature variation"
}
```

### GET /api/alerts/rules
List alert rules.

**Required Permission:** `ALERT_RULE_MANAGE`

### POST /api/alerts/rules
Create alert rule.

**Required Permission:** `ALERT_RULE_MANAGE`

---

## Analytics Service (Port 8085)

### GET /api/analytics/kpis
Get all KPIs.

**Required Permission:** `ANALYTICS_READ`

**Response (200 OK):**
```json
{
  "status": "success",
  "data": [
    {
      "id": 1,
      "name": "system_uptime",
      "displayName": "System Uptime",
      "value": 99.7,
      "unit": "%",
      "target": 99.5,
      "status": "ON_TARGET",
      "trend": "STABLE",
      "category": "RELIABILITY",
      "lastCalculated": "2024-01-15T10:00:00Z"
    },
    {
      "id": 2,
      "name": "avg_response_time",
      "displayName": "Avg Alert Response Time",
      "value": 12.5,
      "unit": "minutes",
      "target": 15,
      "status": "ON_TARGET",
      "trend": "IMPROVING",
      "category": "PERFORMANCE",
      "lastCalculated": "2024-01-15T10:00:00Z"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/analytics/kpis/{name}
Get specific KPI by name.

**Required Permission:** `ANALYTICS_READ`

### GET /api/analytics/dashboard
Get dashboard summary data.

**Required Permission:** `ANALYTICS_READ`

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "totalAssets": 45,
    "totalSensors": 312,
    "activeAlerts": 7,
    "criticalAlerts": 2,
    "healthDistribution": {
      "HEALTHY": 38,
      "WARNING": 5,
      "CRITICAL": 2,
      "OFFLINE": 0
    },
    "assetTypeDistribution": {
      "BRIDGE": 12,
      "TUNNEL": 5,
      "ROAD": 20,
      "DRAINAGE": 8
    },
    "alertTrend": [
      { "date": "2024-01-08", "count": 15 },
      { "date": "2024-01-09", "count": 12 },
      { "date": "2024-01-10", "count": 18 }
    ],
    "topKpis": [...]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### GET /api/analytics/trends
Get trend analysis.

**Required Permission:** `ANALYTICS_READ`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| metric | string | Metric to analyze |
| from | datetime | Start date |
| to | datetime | End date |
| granularity | string | Hour, Day, Week, Month |

### POST /api/analytics/export
Export analytics data.

**Required Permission:** `ANALYTICS_EXPORT`

### POST /api/analytics/refresh
Trigger KPI recalculation.

**Required Permission:** `ANALYTICS_REFRESH`

---

## WebSocket Endpoints

### /ws/sensors
Real-time sensor readings stream.

**Subscribe Topics:**
- `/topic/sensors/{sensorId}` - Individual sensor readings
- `/topic/sensors/all` - All sensor readings

**Message Format:**
```json
{
  "sensorId": 1,
  "value": 125.5,
  "unit": "μm/m",
  "timestamp": "2024-01-15T10:30:00.123Z"
}
```

### /ws/monitoring
Real-time health status updates.

**Subscribe Topics:**
- `/topic/health/{assetId}` - Individual asset health
- `/topic/health/all` - All health updates

### /ws/alerts
Real-time alert notifications.

**Subscribe Topics:**
- `/topic/alerts/new` - New alerts
- `/topic/alerts/updates` - Alert status changes

---

## Error Responses

All endpoints return errors in the following format:

```json
{
  "status": "error",
  "message": "Detailed error message",
  "code": "ERROR_CODE",
  "errors": [
    {
      "field": "fieldName",
      "message": "Field-specific error"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Common Error Codes
| Code | HTTP Status | Description |
|------|-------------|-------------|
| VALIDATION_ERROR | 400 | Invalid request data |
| UNAUTHORIZED | 401 | Missing or invalid token |
| FORBIDDEN | 403 | Insufficient permissions |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Resource conflict |
| INTERNAL_ERROR | 500 | Server error |
| SERVICE_UNAVAILABLE | 503 | Downstream service unavailable |

---

## Rate Limiting

API Gateway enforces rate limiting:
- Default: 100 requests/minute per user
- Authenticated: 1000 requests/minute
- WebSocket: 10 connections per user

Rate limit headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1705315860
```

---

## Pagination

All list endpoints support pagination:

**Request Parameters:**
- `page`: Zero-based page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Response Structure:**
```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```
