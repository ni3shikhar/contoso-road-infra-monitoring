# Road Infrastructure Monitoring - Architecture Documentation

## System Overview

The Road Infrastructure Monitoring system is a full-stack microservices application designed to monitor road infrastructure assets such as bridges, tunnels, roads, and drainage systems. It collects real-time sensor data, processes health metrics, generates alerts, and provides analytics dashboards.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   FRONTEND LAYER                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                     React 18 + TypeScript + Vite + Tailwind                     │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
│  │  │Dashboard │ │ Assets   │ │ Sensors  │ │Monitoring│ │ Alerts   │ │Analytics │ │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
│  │  ┌──────────────────────────────────────────────────────────────────────────┐  │ │
│  │  │         Zustand Store │ TanStack Query │ WebSocket (STOMP)               │  │ │
│  │  └──────────────────────────────────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    API GATEWAY                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │              Spring Cloud Gateway (Port 8080)                                    │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐   │ │
│  │  │   Routing  │ │ Rate Limit │ │   CORS     │ │ JWT Filter │ │Circuit Brkr│   │ │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘ └────────────┘   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
          ┌───────────────────────────────┼───────────────────────────────┐
          ▼                               ▼                               ▼
┌─────────────────┐             ┌─────────────────┐             ┌─────────────────┐
│ Service Registry│             │  Config Server  │             │      Redis      │
│  Eureka (8761)  │             │     (8888)      │             │     (6379)      │
│ ┌─────────────┐ │             │ ┌─────────────┐ │             │ ┌─────────────┐ │
│ │ Service     │ │             │ │ Centralized │ │             │ │ Session     │ │
│ │ Discovery   │ │             │ │ Config      │ │             │ │ Cache       │ │
│ │ Health Check│ │             │ │ Profiles    │ │             │ │ Real-time   │ │
│ └─────────────┘ │             │ └─────────────┘ │             │ └─────────────┘ │
└─────────────────┘             └─────────────────┘             └─────────────────┘
          │
          ├─────────────────────────────────────────────────────────────────────────┐
          │                                                                         │
          │     ┌─────────────────────────────────────────────────────────────┐    │
          │     │                   BUSINESS SERVICES                          │    │
          │     │                                                              │    │
          │     │  ┌───────────┐ ┌───────────┐ ┌──────────────┐              │    │
          │     │  │  Sensor   │ │   Asset   │ │  Monitoring  │              │    │
          │     │  │  Service  │ │  Service  │ │   Service    │              │    │
          │     │  │  (8081)   │ │  (8082)   │ │    (8083)    │              │    │
          │     │  │           │ │           │ │              │              │    │
          │     │  │ sensor_db │ │ asset_db  │ │monitoring_db │              │    │
          │     │  └───────────┘ └───────────┘ └──────────────┘              │    │
          │     │                                                              │    │
          │     │  ┌───────────┐ ┌───────────┐ ┌──────────────┐              │    │
          │     │  │   Alert   │ │ Analytics │ │     Auth     │              │    │
          │     │  │  Service  │ │  Service  │ │   Service    │              │    │
          │     │  │  (8084)   │ │  (8085)   │ │    (8086)    │              │    │
          │     │  │           │ │           │ │              │              │    │
          │     │  │ alert_db  │ │analytics_db│ │   auth_db   │              │    │
          │     │  └───────────┘ └───────────┘ └──────────────┘              │    │
          │     │                                                              │    │
          │     └─────────────────────────────────────────────────────────────┘    │
          │                                                                         │
          └─────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              MESSAGE BROKER                                           │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                        Apache Kafka (9092)                                       │ │
│  │  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐   │ │
│  │  │ sensor-events  │ │ health-events  │ │ alert-events   │ │ asset-events   │   │ │
│  │  └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                               DATA LAYER                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                        PostgreSQL 16 (5432)                                      │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │ │
│  │  │sensor_db │ │ asset_db │ │monitor_db│ │ alert_db │ │analytic_db│ │ auth_db  │ │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Service Descriptions

### Infrastructure Services

#### Service Registry (Eureka - Port 8761)
- Service discovery and registration
- Health monitoring of all microservices
- Load balancing metadata provider

#### Config Server (Port 8888)
- Centralized configuration management
- Environment-specific profiles (dev, docker, prod)
- Native file-based configuration repository

#### API Gateway (Port 8080)
- Single entry point for all client requests
- Route management and load balancing
- JWT authentication filter
- Rate limiting and circuit breaker patterns
- CORS configuration

### Business Services

#### Sensor Service (Port 8081)
- Sensor CRUD operations
- Real-time telemetry data ingestion
- WebSocket streaming of sensor readings
- Kafka producer for sensor events
- Database: sensor_db

#### Asset Service (Port 8082)
- Asset lifecycle management
- Geospatial data support (PostGIS)
- Asset-sensor relationship management
- Asset type categorization (ROAD, BRIDGE, TUNNEL, etc.)
- Database: asset_db

#### Monitoring Service (Port 8083)
- Real-time health status computation
- Threshold-based health assessment
- WebSocket broadcasting of health updates
- Historical health record tracking
- Database: monitoring_db

#### Alert Service (Port 8084)
- Alert generation engine
- Multi-channel notifications (email, SMS)
- Alert lifecycle management (acknowledge, assign, resolve)
- Alert rule configuration
- Database: alert_db

#### Analytics Service (Port 8085)
- KPI computation and aggregation
- Scheduled analytics jobs
- Historical trend analysis
- Dashboard data preparation
- Database: analytics_db

#### Auth Service (Port 8086)
- User authentication (JWT)
- Role-based access control (RBAC)
- Permission management
- Session management with Redis
- Database: auth_db

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|------------|---------|
| Runtime | Java | 21 |
| Framework | Spring Boot | 3.2+ |
| Cloud | Spring Cloud | 2023.0.0 |
| Build | Maven | 3.9+ |
| Database | PostgreSQL | 16 |
| Messaging | Apache Kafka | 7.5.0 |
| Caching | Redis | 7 |
| API Docs | SpringDoc OpenAPI | 3.x |

### Frontend
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | React | 18+ |
| Language | TypeScript | 5.4+ |
| Build | Vite | 5.1+ |
| State | Zustand | 4.5+ |
| Server State | TanStack Query | 5+ |
| Styling | Tailwind CSS | 3.4+ |
| UI Components | shadcn/ui | Latest |
| Routing | React Router | 6+ |
| Charts | Recharts | Latest |
| Maps | Leaflet | Latest |
| WebSocket | STOMP/SockJS | Latest |

## Data Flow

### Sensor Data Ingestion Flow
```
IoT Sensor → Sensor Service → Kafka (sensor-events) → Monitoring Service → Alert Service
                                                              ↓
                                                        Analytics Service
```

### Alert Flow
```
Monitoring Service → Kafka (alert-events) → Alert Service → Notification Channels
                                                    ↓                    ↓
                                              WebSocket Push      Email/SMS
```

### User Request Flow
```
Browser → API Gateway → Service Registry (lookup) → Target Service → Database
              ↓
        JWT Validation
              ↓
        Auth Service
```

## Security Architecture

### Authentication
- JWT-based stateless authentication
- Refresh token rotation
- Session tracking in Redis

### Authorization (RBAC)
- Four roles: ADMIN, ENGINEER, OPERATOR, VIEWER
- Granular permission system
- Method-level security with @RequiresPermission

### Role-Permission Matrix
| Role | Permissions |
|------|-------------|
| ADMIN | All permissions |
| ENGINEER | Sensor R/W/Configure, Asset R/W, Monitoring R/Configure, Alert Full, Analytics Full, Inspection R/W |
| OPERATOR | Sensor R, Asset R/Progress, Monitoring R, Alert R/Ack/Assign/Resolve, Analytics R, Inspection R/W |
| VIEWER | Read-only access to all resources |

## Deployment Architecture

### Docker Compose (Development/Staging)
- All services containerized
- Multi-stage Docker builds
- Health checks on all containers
- Shared Docker network
- Persistent volumes for databases

### Production Recommendations
- Kubernetes deployment with Helm charts
- Horizontal pod autoscaling
- Service mesh (Istio) for observability
- External secrets management
- Multi-zone PostgreSQL replication
- Kafka cluster with 3+ brokers
- Redis Sentinel for high availability

## Monitoring & Observability

### Health Endpoints
- All services expose `/actuator/health`
- Custom health indicators for external dependencies

### Metrics
- Micrometer metrics exported to Prometheus
- JVM, HTTP, and custom business metrics

### Logging
- Structured JSON logging
- Correlation ID propagation across services
- Log aggregation via ELK stack recommended

### Tracing
- Spring Cloud Sleuth for distributed tracing
- Zipkin integration for trace visualization

## Scalability Considerations

### Horizontal Scaling
- Stateless services allow easy scaling
- Redis handles distributed session state
- Kafka partitioning for parallel processing

### Database Scaling
- Read replicas for reporting queries
- Connection pooling (HikariCP)
- Query optimization with indexes

### Caching Strategy
- Redis for frequently accessed data
- Cache-aside pattern
- TTL-based cache invalidation
