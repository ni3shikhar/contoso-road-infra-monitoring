# Road Infrastructure Monitoring System

A comprehensive full-stack microservices application for monitoring road infrastructure assets, sensors, and system health with real-time alerting, analytics, and role-based access control.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Service Catalog](#service-catalog)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Build Instructions](#build-instructions)
- [Deployment Guide](#deployment-guide)
- [Service Details](#service-details)
- [API Endpoints](#api-endpoints)
- [Role-Based Access Control](#role-based-access-control)
- [Default Credentials](#default-credentials)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Frontend (React)                                │
│                    React 18 + TypeScript + Vite + Tailwind                   │
│                              Port: 3000                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (8080)                                 │
│                 Spring Cloud Gateway + JWT Auth + Rate Limiting              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ Service Registry│         │  Config Server  │         │     Redis       │
│  Eureka (8761)  │         │     (8888)      │         │    (6379)       │
└─────────────────┘         └─────────────────┘         └─────────────────┘
          │
          ├──────────────────────────────────────────────────────────────┐
          │                                                              │
┌─────────┴─────────┬─────────────────┬─────────────────┬───────────────┴───┐
│                   │                 │                 │                   │
▼                   ▼                 ▼                 ▼                   ▼
┌───────────┐ ┌───────────┐ ┌──────────────┐ ┌───────────┐ ┌──────────────┐
│  Sensor   │ │   Asset   │ │  Monitoring  │ │   Alert   │ │  Analytics   │
│  Service  │ │  Service  │ │   Service    │ │  Service  │ │   Service    │
│  (8081)   │ │  (8082)   │ │    (8083)    │ │  (8084)   │ │    (8085)    │
└───────────┘ └───────────┘ └──────────────┘ └───────────┘ └──────────────┘
      │             │              │               │              │
      ▼             ▼              ▼               ▼              ▼
┌───────────┐ ┌───────────┐ ┌──────────────┐ ┌───────────┐ ┌──────────────┐
│ sensor_db │ │ asset_db  │ │monitoring_db │ │ alert_db  │ │analytics_db  │
└───────────┘ └───────────┘ └──────────────┘ └───────────┘ └──────────────┘

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Auth Service   │    │    Simulator    │    │   Kafka/ZK      │
│     (8086)      │    │     (9000)      │    │  (9092/2181)    │
└────────┬────────┘    └─────────────────┘    └─────────────────┘
         │
         ▼
┌─────────────────┐
│    auth_db      │
└─────────────────┘
```

---

## Service Catalog

### Infrastructure Services

| Service | Port | Container Name | Description | Database |
|---------|------|----------------|-------------|----------|
| **PostgreSQL** | 5432 | road-infra-postgres | Primary database with PostGIS extension | 6 schemas |
| **Redis** | 6379 | road-infra-redis | Caching, session management, rate limiting | - |
| **Kafka** | 9092 | road-infra-kafka | Event streaming, inter-service messaging | - |
| **Zookeeper** | 2181 | road-infra-zookeeper | Kafka coordination | - |

### Spring Cloud Infrastructure

| Service | Port | Container Name | Description | Health Check |
|---------|------|----------------|-------------|--------------|
| **Service Registry** | 8761 | road-infra-service-registry | Eureka Server for service discovery | `/actuator/health` |
| **Config Server** | 8888 | road-infra-config-server | Centralized configuration management | `/actuator/health` |
| **API Gateway** | 8080 | road-infra-api-gateway | Single entry point, JWT validation, routing | `/actuator/health` |

### Business Microservices

| Service | Port | Container Name | Description | Database | Kafka Topics |
|---------|------|----------------|-------------|----------|--------------|
| **Auth Service** | 8086 | road-infra-auth-service | Authentication, authorization, user management | auth_db | auth-events |
| **Sensor Service** | 8081 | road-infra-sensor-service | Sensor CRUD, readings, calibration | sensor_db | sensor-readings, sensor-events |
| **Asset Service** | 8082 | road-infra-asset-service | Asset management, construction progress | asset_db | asset-events |
| **Monitoring Service** | 8083 | road-infra-monitoring-service | Health scores, threshold monitoring | monitoring_db | health-events |
| **Alert Service** | 8084 | road-infra-alert-service | Alert generation, escalation, notifications | alert_db | alert-events |
| **Analytics Service** | 8085 | road-infra-analytics-service | KPIs, dashboards, trend analysis | analytics_db | analytics-events |

### Supporting Services

| Service | Port | Container Name | Description |
|---------|------|----------------|-------------|
| **Frontend** | 3000 | road-infra-frontend | React SPA (served via Nginx) |
| **Simulator** | 9000 | road-infra-simulator | Generates test sensor readings and alerts |

---

## Tech Stack

### Backend
- **Java 21** - Latest LTS version
- **Spring Boot 3.2.2** - Application framework
- **Spring Cloud 2023.0.0** - Microservices infrastructure
  - Spring Cloud Gateway - API routing and filtering
  - Spring Cloud Netflix Eureka - Service discovery
  - Spring Cloud Config - Centralized configuration
  - Spring Cloud OpenFeign - Declarative REST clients
  - Resilience4j - Circuit breaker and fault tolerance
- **PostgreSQL 16** - Primary database (6 separate schemas)
- **Apache Kafka** - Event streaming and inter-service messaging
- **Redis 7** - Caching and session management
- **JJWT 0.12.3** - JWT authentication

### Frontend
- **React 18** - UI framework
- **TypeScript 5.4** - Type-safe JavaScript
- **Vite 5.1** - Build tool
- **Zustand 4.5** - State management
- **TanStack Query 5** - Server state management
- **Tailwind CSS 3.4** - Utility-first CSS
- **shadcn/ui** - UI component library
- **React Router v6** - Client-side routing
- **Recharts** - Data visualization
- **Leaflet** - Interactive maps
- **WebSocket (STOMP)** - Real-time communication

## Project Structure

```
road-infra-monitor/
├── backend/
│   ├── pom.xml                    # Parent POM
│   ├── common-lib/                # Shared library
│   ├── service-registry/          # Eureka Server
│   ├── config-server/             # Spring Cloud Config
│   ├── api-gateway/               # API Gateway
│   ├── sensor-service/            # Sensor management
│   ├── asset-service/             # Asset management
│   ├── monitoring-service/        # Health monitoring
│   ├── alert-service/             # Alert management
│   ├── analytics-service/         # KPI analytics
│   └── auth-service/              # Authentication
├── frontend/
│   ├── src/
│   │   ├── components/            # React components
│   │   ├── pages/                 # Page components
│   │   ├── services/              # API services
│   │   ├── hooks/                 # Custom hooks
│   │   ├── store/                 # Zustand stores
│   │   └── types/                 # TypeScript types
│   ├── package.json
│   └── vite.config.ts
├── infrastructure/
│   ├── docker/                    # Dockerfiles for each service
│   ├── docker-compose.yml         # Full stack orchestration
│   └── init-scripts/              # DB initialization scripts
└── docs/                          # Architecture & API specs
```

## Role-Based Access Control (RBAC)

The system implements a comprehensive RBAC model with four roles and granular permissions.

### Roles

| Role | Description |
|------|-------------|
| **ADMIN** | Full system access including user management |
| **ENGINEER** | Technical access for sensor/asset configuration and analytics |
| **OPERATOR** | Operational access for monitoring and incident response |
| **VIEWER** | Read-only access to all resources |

### Persona-to-Role Mapping

| Job Title / Persona | Assigned Role | Primary Responsibilities |
|---------------------|---------------|--------------------------|
| Site/Project Manager | OPERATOR | Progress tracking, alert management, inspections |
| Structural/Civil Engineer | ENGINEER | Structural data analysis, threshold configuration |
| Maintenance/Ops Manager | OPERATOR | Health data monitoring, maintenance scheduling |
| Safety Officer | OPERATOR | Real-time safety alerts, compliance monitoring |
| IoT/Instrumentation Tech | ENGINEER | Sensor management, calibration, diagnostics |
| Executive/Project Sponsor | VIEWER | Dashboards, KPIs, high-level reports |
| Regulatory Inspector | VIEWER | Read-only inspections, compliance data |
| Data Analyst/Asset Planner | ENGINEER | Analytics, exports, historical analysis |

### Permission Matrix Summary

| Permission Area | ADMIN | ENGINEER | OPERATOR | VIEWER |
|----------------|:-----:|:--------:|:--------:|:------:|
| Sensor Read | ✓ | ✓ | ✓ | ✓ |
| Sensor Write/Configure | ✓ | ✓ | | |
| Asset Read | ✓ | ✓ | ✓ | ✓ |
| Asset Write | ✓ | ✓ | | |
| Asset Progress Update | ✓ | ✓ | ✓ | |
| Monitoring Read | ✓ | ✓ | ✓ | ✓ |
| Monitoring Configure | ✓ | ✓ | | |
| Alert Read | ✓ | ✓ | ✓ | ✓ |
| Alert Manage | ✓ | ✓ | ✓ | |
| Alert Rules | ✓ | ✓ | | |
| Analytics Read | ✓ | ✓ | ✓ | ✓ |
| Analytics Export | ✓ | ✓ | | |
| Inspection Read/Write | ✓ | ✓ | ✓ | Read |
| User Management | ✓ | | | |
| System Admin | ✓ | | | |

> **Note:** See [docs/RBAC.md](docs/RBAC.md) for complete permission details and implementation guide.

---

## Prerequisites

### Required Software

| Software | Version | Purpose | Download |
|----------|---------|---------|----------|
| **Java JDK** | 21+ (LTS) | Backend services | [Eclipse Temurin](https://adoptium.net/) |
| **Node.js** | 20+ (LTS) | Frontend build | [nodejs.org](https://nodejs.org/) |
| **Docker** | 24+ | Containerization | [docker.com](https://www.docker.com/) |
| **Docker Compose** | 2.20+ | Multi-container orchestration | Included with Docker Desktop |
| **Maven** | 3.9+ | Java build tool | [maven.apache.org](https://maven.apache.org/) (or use included `mvnw`) |
| **Git** | 2.40+ | Version control | [git-scm.com](https://git-scm.com/) |

### Verify Installation

```bash
# Check all prerequisites
java -version          # Should show: openjdk 21.x.x
node -v                # Should show: v20.x.x or higher
npm -v                 # Should show: 10.x.x or higher
docker --version       # Should show: Docker version 24.x.x
docker compose version # Should show: Docker Compose version v2.20.x
mvn -v                 # Should show: Apache Maven 3.9.x
```

### System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 8 GB | 16 GB |
| CPU | 4 cores | 8 cores |
| Disk | 20 GB free | 50 GB free |
| OS | Windows 10/11, macOS 12+, Ubuntu 22.04+ | - |

---

## Build Instructions

### Step 1: Clone the Repository

```bash
git clone https://github.com/ni3shikhar/contoso-road-infra-monitoring.git
cd contoso-road-infra-monitoring/road-infra-monitor
```

### Step 2: Build Backend Services

```bash
# Navigate to backend directory
cd backend

# Build all services (skip tests for faster build)
./mvnw clean package -DskipTests

# OR build with tests
./mvnw clean package

# Verify build output
ls -la */target/*.jar
```

**Expected Output:**
```
service-registry/target/service-registry-1.0.0.jar
config-server/target/config-server-1.0.0.jar
api-gateway/target/api-gateway-1.0.0.jar
auth-service/target/auth-service-1.0.0.jar
sensor-service/target/sensor-service-1.0.0.jar
asset-service/target/asset-service-1.0.0.jar
monitoring-service/target/monitoring-service-1.0.0.jar
alert-service/target/alert-service-1.0.0.jar
analytics-service/target/analytics-service-1.0.0.jar
```

### Step 3: Build Frontend

```bash
# Navigate to frontend directory
cd ../frontend

# Install dependencies
npm install

# Build for production
npm run build

# Verify build output
ls -la dist/
```

### Step 4: Build Docker Images

```bash
# Navigate to infrastructure directory
cd ../infrastructure

# Build all Docker images
docker compose build

# Verify images
docker images | grep road-infra
```

---

## Deployment Guide

### Option 1: Docker Compose (Recommended)

#### Step 1: Start All Services

```bash
cd infrastructure

# Start all services in detached mode
docker compose up -d

# Watch logs
docker compose logs -f
```

#### Step 2: Verify Services Health

```bash
# Check container status
docker compose ps

# Check individual service health
docker compose logs service-registry
docker compose logs api-gateway
```

#### Step 3: Wait for Services to Initialize

Services start in order with health checks. Full startup takes **3-5 minutes**.

```bash
# Monitor startup progress
watch -n 5 'docker compose ps'
```

#### Step 4: Access the Application

| Service | URL | Credentials |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | admin / Admin@123 |
| **API Gateway** | http://localhost:8080 | - |
| **Eureka Dashboard** | http://localhost:8761 | - |
| **Config Server** | http://localhost:8888 | - |

#### Step 5: Seed Test Data (Optional)

```bash
# Windows PowerShell
./seed-data.ps1

# Linux/macOS
./seed-data.sh
```

#### Stopping Services

```bash
# Stop all services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v
```

---

### Option 2: Local Development

#### Step 1: Start Infrastructure Services Only

```bash
cd infrastructure

# Start only infrastructure (PostgreSQL, Kafka, Redis, Zookeeper)
docker compose up -d postgres kafka zookeeper redis

# Verify infrastructure is healthy
docker compose ps
```

#### Step 2: Start Spring Cloud Services (Order Matters!)

Open **separate terminals** for each service:

```bash
cd backend

# Terminal 1 - Service Registry (MUST START FIRST)
./mvnw spring-boot:run -pl service-registry
# Wait until: "Started ServiceRegistryApplication" appears

# Terminal 2 - Config Server (MUST START SECOND)
./mvnw spring-boot:run -pl config-server
# Wait until: "Started ConfigServerApplication" appears

# Terminal 3 - API Gateway
./mvnw spring-boot:run -pl api-gateway
# Wait until: "Started ApiGatewayApplication" appears
```

#### Step 3: Start Business Services

```bash
# Terminal 4 - Auth Service (START BEFORE OTHERS)
./mvnw spring-boot:run -pl auth-service

# Terminals 5-9 - Can start in parallel
./mvnw spring-boot:run -pl sensor-service
./mvnw spring-boot:run -pl asset-service
./mvnw spring-boot:run -pl monitoring-service
./mvnw spring-boot:run -pl alert-service
./mvnw spring-boot:run -pl analytics-service
```

#### Step 4: Start Frontend

```bash
cd frontend

# Development mode with hot reload
npm run dev
```

#### Service Startup Order Summary

```
1. PostgreSQL, Kafka, Redis, Zookeeper (infrastructure)
2. Service Registry (Eureka)
3. Config Server
4. API Gateway
5. Auth Service
6. Sensor, Asset, Monitoring, Alert, Analytics Services (parallel)
7. Frontend
8. Simulator (optional)
```

---

### Option 3: Hybrid Development

Run infrastructure and Spring Cloud services in Docker, develop specific service locally:

```bash
cd infrastructure

# Start everything except the service you're developing
docker compose up -d postgres kafka zookeeper redis service-registry config-server api-gateway auth-service

# Run your service locally for development
cd ../backend
./mvnw spring-boot:run -pl sensor-service -Dspring-boot.run.profiles=local
```

---

### Startup Verification Checklist

| Step | Check | Expected Result |
|------|-------|-----------------|
| 1 | `docker compose ps` | All containers show "healthy" |
| 2 | http://localhost:8761 | Eureka shows all services registered |
| 3 | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| 4 | http://localhost:3000 | Login page loads |
| 5 | Login as admin/Admin@123 | Dashboard displays |

---

## Service Details

### Auth Service (Port 8086)

**Purpose:** User authentication, JWT token management, RBAC

**Key Features:**
- JWT-based authentication with refresh tokens
- Role-based access control (4 roles, 22 permissions)
- Account lockout after failed attempts
- Password policy enforcement
- Audit logging

**Database:** `auth_db`
- Tables: users, refresh_tokens, audit_logs

**API Base Path:** `/api/v1/auth`

---

### Sensor Service (Port 8081)

**Purpose:** Sensor lifecycle management, readings collection

**Key Features:**
- CRUD operations for sensors
- Real-time sensor readings via Kafka
- Calibration tracking
- Threshold configuration
- Batch reading submission

**Database:** `sensor_db`
- Tables: sensors, sensor_readings, sensor_calibrations

**Kafka Topics:**
- `sensor-readings` - Real-time sensor data
- `sensor-events` - Lifecycle events

**API Base Path:** `/api/v1/sensors`

---

### Asset Service (Port 8082)

**Purpose:** Road infrastructure asset management

**Key Features:**
- Asset CRUD with geospatial data (PostGIS)
- Construction progress tracking
- Asset-sensor relationships
- Inspection scheduling

**Database:** `asset_db`
- Tables: assets, asset_sensors, inspections

**API Base Path:** `/api/v1/assets`

---

### Monitoring Service (Port 8083)

**Purpose:** System health monitoring and scoring

**Key Features:**
- Real-time health score calculation
- Threshold-based monitoring
- Health history tracking
- Anomaly detection

**Database:** `monitoring_db`
- Tables: health_records, thresholds, anomalies

**Kafka Topics:**
- `health-events` - Health score changes

**API Base Path:** `/api/v1/monitoring`

---

### Alert Service (Port 8084)

**Purpose:** Alert generation, escalation, and management

**Key Features:**
- Automated alert generation from sensor data
- Severity levels (INFO, WARNING, CRITICAL)
- Alert acknowledgment and resolution workflow
- Escalation rules
- Notification integration

**Database:** `alert_db`
- Tables: alerts, alert_rules, escalations

**Kafka Topics:**
- `alert-events` - Alert notifications

**API Base Path:** `/api/v1/alerts`

---

### Analytics Service (Port 8085)

**Purpose:** KPIs, dashboards, and trend analysis

**Key Features:**
- Pre-computed KPI metrics
- Historical trend analysis
- Dashboard data aggregation
- Export to CSV/Excel

**Database:** `analytics_db`
- Tables: kpis, trends, dashboard_metrics

**API Base Path:** `/api/v1/analytics`

---

### Simulator Service (Port 9000)

**Purpose:** Generate realistic test data

**Key Features:**
- Automated sensor reading generation
- Configurable failure injection
- Anomaly simulation
- Load testing support

**Configuration:**
```yaml
SIMULATION_ENABLED: true
READING_INTERVAL: 10      # seconds
FAILURE_INTERVAL: 60      # seconds
ANOMALY_INTERVAL: 120     # seconds
```

---

### Frontend (Port 3000)

**Purpose:** User interface

**Key Features:**
- Real-time dashboard with WebSocket updates
- Interactive corridor map (Leaflet)
- Sensor and asset management
- Alert management console
- Analytics visualizations (Recharts)
- Responsive design (Tailwind CSS)

**Pages:**
- `/` - Dashboard
- `/sensors` - Sensor management
- `/assets` - Asset management
- `/alerts` - Alert console
- `/analytics` - Analytics dashboard
- `/map` - Corridor map
- `/settings` - User settings

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User login |
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/refresh` | Refresh JWT token |
| POST | `/api/auth/logout` | User logout |
| GET | `/api/auth/me` | Get current user |

### Sensors
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sensors` | List all sensors |
| GET | `/api/sensors/{id}` | Get sensor by ID |
| POST | `/api/sensors` | Create sensor |
| PUT | `/api/sensors/{id}` | Update sensor |
| DELETE | `/api/sensors/{id}` | Delete sensor |
| GET | `/api/sensors/{id}/readings` | Get sensor readings |

### Assets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/assets` | List all assets |
| GET | `/api/assets/{id}` | Get asset by ID |
| POST | `/api/assets` | Create asset |
| PUT | `/api/assets/{id}` | Update asset |
| DELETE | `/api/assets/{id}` | Delete asset |
| GET | `/api/assets/{id}/sensors` | Get asset sensors |

### Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/monitoring/health` | Get system health |
| GET | `/api/monitoring/health/{assetId}` | Get asset health |
| GET | `/api/monitoring/history` | Get health history |

### Alerts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alerts` | List all alerts |
| GET | `/api/alerts/{id}` | Get alert by ID |
| PUT | `/api/alerts/{id}/acknowledge` | Acknowledge alert |
| PUT | `/api/alerts/{id}/resolve` | Resolve alert |
| GET | `/api/alerts/active` | Get active alerts |

### Analytics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/kpis` | Get all KPIs |
| GET | `/api/analytics/kpis/{name}` | Get KPI by name |
| GET | `/api/analytics/dashboard` | Get dashboard data |
| GET | `/api/analytics/trends` | Get trend analysis |

## WebSocket Endpoints

- `/ws/sensors` - Real-time sensor readings
- `/ws/monitoring` - Real-time health updates
- `/ws/alerts` - Real-time alert notifications

## Environment Variables

### Backend Services
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka server URL | `http://localhost:8761/eureka/` |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | varies by service |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `JWT_SECRET` | JWT signing secret | required |

### Frontend
| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Backend API URL | `http://localhost:8080/api` |
| `VITE_WS_URL` | WebSocket URL | `ws://localhost:8080/ws` |

## Service Ports Summary

| Category | Service | Port | Protocol |
|----------|---------|------|----------|
| **Frontend** | React App (Nginx) | 3000 | HTTP |
| **API** | API Gateway | 8080 | HTTP |
| **Spring Cloud** | Service Registry (Eureka) | 8761 | HTTP |
| | Config Server | 8888 | HTTP |
| **Business** | Sensor Service | 8081 | HTTP |
| | Asset Service | 8082 | HTTP |
| | Monitoring Service | 8083 | HTTP |
| | Alert Service | 8084 | HTTP |
| | Analytics Service | 8085 | HTTP |
| | Auth Service | 8086 | HTTP |
| **Support** | Simulator | 9000 | HTTP |
| **Database** | PostgreSQL | 5432 | TCP |
| **Cache** | Redis | 6379 | TCP |
| **Messaging** | Kafka | 9092, 29092 | TCP |
| | Zookeeper | 2181 | TCP |

---

## Default Credentials

### Application Users

| Username | Password | Role | Persona | Department |
|----------|----------|------|---------|------------|
| admin | Admin@123 | ADMIN | System Administrator | IT Department |
| j.engineer | Eng@123 | ENGINEER | Structural Engineer | Engineering |
| s.iottech | IoT@123 | ENGINEER | IoT Technician | Engineering |
| d.analyst | Data@123 | ENGINEER | Data Analyst | Planning |
| m.projmgr | Proj@123 | OPERATOR | Site Project Manager | Construction |
| k.maintenance | Maint@123 | OPERATOR | Maintenance Manager | Operations |
| r.safety | Safe@123 | OPERATOR | Safety Officer | Safety |
| e.director | Exec@123 | VIEWER | Executive Director | Management |
| i.inspector | Insp@123 | VIEWER | Regulatory Inspector | Compliance |

### Infrastructure

| Service | Username | Password |
|---------|----------|----------|
| PostgreSQL | postgres | postgres |
| Redis | - | - (no auth) |

---

## Troubleshooting

### Common Issues

#### 1. Services Not Starting

**Symptom:** Services fail to start or remain unhealthy

**Solution:**
```bash
# Check logs for specific service
docker compose logs <service-name>

# Restart the problematic service
docker compose restart <service-name>

# If Eureka is down, restart in order
docker compose restart service-registry
docker compose restart config-server
docker compose restart api-gateway
```

#### 2. Database Connection Issues

**Symptom:** `Connection refused` to PostgreSQL

**Solution:**
```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check PostgreSQL logs
docker compose logs postgres

# Verify databases were created
docker compose exec postgres psql -U postgres -c "\l"
```

#### 3. Kafka Connection Issues

**Symptom:** Services can't connect to Kafka

**Solution:**
```bash
# Check Kafka and Zookeeper status
docker compose ps kafka zookeeper

# Restart Kafka stack
docker compose restart zookeeper kafka
```

#### 4. Frontend Can't Connect to Backend

**Symptom:** Login fails or API calls timeout

**Solution:**
1. Verify API Gateway is running: http://localhost:8080/actuator/health
2. Check browser console for CORS errors
3. Verify auth-service is registered in Eureka: http://localhost:8761

#### 5. Out of Memory

**Symptom:** Containers keep restarting

**Solution:**
```bash
# Increase Docker memory (Docker Desktop settings)
# Recommended: 8GB minimum

# Or reduce services running
docker compose up -d postgres kafka redis service-registry config-server api-gateway auth-service frontend
```

#### 6. Port Already in Use

**Symptom:** `port is already allocated`

**Solution:**
```bash
# Find process using the port (Windows)
netstat -ano | findstr :<PORT>
taskkill /PID <PID> /F

# Find process using the port (Linux/macOS)
lsof -i :<PORT>
kill -9 <PID>
```

### Useful Commands

```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f sensor-service

# Restart all services
docker compose restart

# Clean restart (removes data)
docker compose down -v && docker compose up -d

# Check service health
curl http://localhost:8080/actuator/health

# Test authentication
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

---

## Testing

### Backend
```bash
cd backend
./mvnw test
```

### Frontend
```bash
cd frontend
npm run test
```

## Building for Production

### Backend
```bash
cd backend
./mvnw clean package -DskipTests
```

### Frontend
```bash
cd frontend
npm run build
```

### Docker Images
```bash
docker-compose build
```

---

## Integration Tests

See [integration-tests/README.md](integration-tests/README.md) for comprehensive end-to-end and RBAC integration tests.

```bash
cd integration-tests

# Run all integration tests (requires services running)
mvn failsafe:integration-test -Pintegration-tests

# Run specific tests
mvn failsafe:integration-test -Dit.test=EndToEndFlowIT
mvn failsafe:integration-test -Dit.test=RbacIntegrationIT
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Detailed system architecture |
| [API-SPECIFICATION.md](docs/API-SPECIFICATION.md) | Complete API documentation |
| [RBAC.md](docs/RBAC.md) | Role-based access control details |
| [integration-tests/README.md](integration-tests/README.md) | Integration test guide |

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Support

For issues and questions:
- Create a GitHub Issue
- Check existing documentation in `/docs`
- Review troubleshooting section above
