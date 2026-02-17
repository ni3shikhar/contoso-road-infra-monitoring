# Road Infrastructure Data Simulator

A standalone Spring Boot application that generates realistic sensor data for the Road Infrastructure Monitoring System.

## Overview

The simulator creates continuous, realistic sensor readings that mimic actual road corridor monitoring equipment. It includes:

- **Realistic Data Patterns**: Diurnal cycles, traffic-based variations, environmental correlations
- **Anomaly Injection**: Periodic introduction of gradual drift, sudden spikes, stuck values, and erratic readings
- **Failure Simulation**: Random sensor failures and recovery patterns
- **Multiple Sensor Types**: Support for all 12 sensor types in the system

## Sensor Types Supported

| Type | Unit | Normal Range | Description |
|------|------|--------------|-------------|
| Strain Gauge | μstrain | 50-200 | Structural stress measurement |
| Accelerometer | g | 0.01-0.05 | Vibration/movement detection |
| Temperature | °C | 15-35 | Ambient temperature (diurnal pattern) |
| Displacement | mm | 0-2 | Structural movement |
| Crack Meter | mm | 0-1 | Crack width monitoring |
| Tiltmeter | degrees | -0.5-0.5 | Inclination changes |
| GPS | degrees | -180-180 | Position tracking |
| Moisture | %RH | 30-70 | Humidity/moisture levels |
| Air Quality | ppm | 5-25 | Air pollution levels |
| Flow Sensor | L/min | 10-100 | Drainage flow rates |
| Level Sensor | m | 0.5-2.0 | Water/drainage levels |
| Impact Sensor | N | 0-100 | Impact detection |

## Configuration

### Application Properties

```yaml
simulator:
  enabled: true                    # Enable/disable the simulator
  reading-interval-ms: 10000       # Reading generation interval (10 seconds)
  failure-interval-ms: 3600000     # Failure simulation interval (1 hour)
  anomaly-interval-ms: 7200000     # Anomaly injection interval (2 hours)
  anomaly-probability: 0.1         # 10% of sensors get anomalies
  failure-probability: 0.05        # 5% of sensors fail

api:
  gateway-url: http://localhost:8080  # API Gateway URL
  auth:
    username: admin
    password: Admin@123
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SIMULATOR_ENABLED` | Enable/disable simulator | true |
| `SIMULATOR_READING_INTERVAL_MS` | Reading interval in ms | 10000 |
| `SIMULATOR_FAILURE_INTERVAL_MS` | Failure simulation interval | 3600000 |
| `SIMULATOR_ANOMALY_INTERVAL_MS` | Anomaly injection interval | 7200000 |
| `SIMULATOR_API_GATEWAY_URL` | API Gateway URL | http://localhost:8080 |
| `SIMULATOR_AUTH_USERNAME` | Auth username | admin |
| `SIMULATOR_AUTH_PASSWORD` | Auth password | Admin@123 |

## Running the Simulator

### Standalone

```bash
cd simulator
mvn spring-boot:run
```

### With Docker Compose

The simulator is included in the infrastructure docker-compose.yml:

```bash
cd infrastructure
docker-compose up simulator
```

### Building Docker Image

```bash
docker build -f infrastructure/docker/simulator.Dockerfile -t road-infra-simulator:latest .
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/simulator/status` | GET | Get simulator status and statistics |
| `/actuator/health` | GET | Health check |

### Status Response Example

```json
{
  "status": "RUNNING",
  "simulatedSensors": 45,
  "totalReadingsGenerated": 12450,
  "lastReadingTime": "2024-01-15T10:30:00Z",
  "activeAnomalies": 3,
  "failedSensors": 2
}
```

## Anomaly Types

The simulator can inject the following anomaly types:

1. **GRADUAL_DRIFT**: Slow drift away from normal values
2. **SUDDEN_SPIKE**: Sudden large changes in readings
3. **STUCK_VALUE**: Sensor returns same value repeatedly
4. **ERRATIC_NOISE**: Random noise added to readings

## Data Generation Patterns

### Diurnal Patterns
Temperature sensors follow a realistic daily cycle:
- Morning warming (6:00-12:00)
- Afternoon peak (12:00-15:00)
- Evening cooling (15:00-21:00)
- Night minimum (21:00-6:00)

### Traffic-Based Variations
Strain gauges and accelerometers vary based on simulated traffic:
- Rush hour peaks (7:00-9:00, 17:00-19:00)
- Reduced traffic at night
- Weekend vs weekday patterns

### Environmental Correlations
- Moisture affects crack meter readings
- Temperature affects strain measurements
- Air quality varies with traffic

## Architecture

```
simulator/
├── src/main/java/com/contoso/roadinfra/simulator/
│   ├── SimulatorApplication.java       # Main application
│   ├── config/
│   │   ├── SimulatorConfig.java        # Simulation settings
│   │   ├── ApiConfig.java              # API connection config
│   │   └── WebClientConfig.java        # HTTP client config
│   ├── model/
│   │   ├── SensorType.java             # Sensor type enum
│   │   ├── SensorStatus.java           # Status enum
│   │   ├── AnomalyType.java            # Anomaly types
│   │   └── SimulatedSensor.java        # Simulated sensor model
│   ├── client/
│   │   ├── AuthClient.java             # Authentication client
│   │   ├── SensorServiceClient.java    # Sensor API client
│   │   └── AssetServiceClient.java     # Asset API client
│   ├── service/
│   │   ├── SensorDataGenerator.java    # Data generation logic
│   │   └── SimulationScheduler.java    # Main scheduler
│   └── controller/
│       └── SimulatorController.java    # Status endpoint
└── src/main/resources/
    └── application.yml                 # Configuration
```

## Dependencies

- Spring Boot 3.2.2
- Spring WebFlux (WebClient)
- Spring Boot Actuator
- Lombok
- Jackson (JSON)

## Monitoring

The simulator exposes metrics via Spring Actuator:

- `/actuator/metrics/simulator.readings.generated`
- `/actuator/metrics/simulator.anomalies.active`
- `/actuator/metrics/simulator.sensors.failed`
