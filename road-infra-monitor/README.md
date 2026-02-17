# Road Infrastructure Monitoring System

A full-stack microservices application for monitoring road infrastructure assets, sensors, and system health with real-time alerting and analytics.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Frontend (React)                                │
│                    React 18 + TypeScript + Vite + Tailwind                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (8080)                                 │
│                      Spring Cloud Gateway + JWT Auth                         │
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

                              ┌─────────────────┐
                              │  Auth Service   │
                              │     (8086)      │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │    auth_db      │
                              └─────────────────┘
```

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

## Getting Started

### Prerequisites
- Java 21 JDK
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### Quick Start with Docker

1. Clone the repository:
```bash
git clone <repository-url>
cd road-infra-monitor
```

2. Start all services with Docker Compose:
```bash
docker-compose up -d
```

3. Access the application:
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **Config Server**: http://localhost:8888

### Local Development

#### Backend

1. Start infrastructure services:
```bash
docker-compose up -d postgres kafka zookeeper redis
```

2. Start services in order:
```bash
# Terminal 1 - Service Registry
cd backend
./mvnw spring-boot:run -pl service-registry

# Terminal 2 - Config Server
./mvnw spring-boot:run -pl config-server

# Terminal 3 - API Gateway
./mvnw spring-boot:run -pl api-gateway

# Terminal 4-9 - Business Services
./mvnw spring-boot:run -pl sensor-service
./mvnw spring-boot:run -pl asset-service
./mvnw spring-boot:run -pl monitoring-service
./mvnw spring-boot:run -pl alert-service
./mvnw spring-boot:run -pl analytics-service
./mvnw spring-boot:run -pl auth-service
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

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

## Service Ports

| Service | Port |
|---------|------|
| Service Registry (Eureka) | 8761 |
| Config Server | 8888 |
| API Gateway | 8080 |
| Sensor Service | 8081 |
| Asset Service | 8082 |
| Monitoring Service | 8083 |
| Alert Service | 8084 |
| Analytics Service | 8085 |
| Auth Service | 8086 |
| Frontend | 3000 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| Zookeeper | 2181 |

## Default Credentials

- **Admin User**: admin / admin123
- **PostgreSQL**: postgres / postgres
- **Eureka Security**: eureka / eureka123

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

## License

This project is licensed under the MIT License.
