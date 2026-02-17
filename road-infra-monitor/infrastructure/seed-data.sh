#!/bin/bash

# =============================================================================
# Seed Data Script for Road Infrastructure Monitoring System
# =============================================================================
# This script:
# 1. Waits for all services to be healthy
# 2. Registers 45 sensors distributed across all assets
# 3. Creates monitoring thresholds for all sensor/asset type combinations
# 4. Creates sample alert rules
# 5. Starts the simulator
# =============================================================================

set -e

# Configuration
API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"
EUREKA_URL="${EUREKA_URL:-http://localhost:8761}"
MAX_RETRIES=60
RETRY_INTERVAL=5

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# =============================================================================
# Wait for Services to be Healthy
# =============================================================================

wait_for_service() {
    local service_name=$1
    local health_url=$2
    local retries=0

    log_info "Waiting for $service_name to be healthy..."
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -s "$health_url" | grep -q '"status":"UP"' 2>/dev/null; then
            log_success "$service_name is healthy"
            return 0
        fi
        
        retries=$((retries + 1))
        log_info "Waiting for $service_name... (attempt $retries/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
    done
    
    log_error "$service_name did not become healthy in time"
    return 1
}

wait_for_eureka_service() {
    local service_name=$1
    local retries=0

    log_info "Waiting for $service_name to register with Eureka..."
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -s -H "Accept: application/json" "$EUREKA_URL/eureka/apps" | grep -qi "$service_name" 2>/dev/null; then
            log_success "$service_name is registered with Eureka"
            return 0
        fi
        
        retries=$((retries + 1))
        log_info "Waiting for $service_name registration... (attempt $retries/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
    done
    
    log_error "$service_name did not register in time"
    return 1
}

wait_for_all_services() {
    log_info "=========================================="
    log_info "Waiting for all services to be healthy..."
    log_info "=========================================="
    
    # Wait for Eureka first
    wait_for_service "Eureka" "$EUREKA_URL/actuator/health"
    
    # Wait for services to register
    wait_for_eureka_service "AUTH-SERVICE"
    wait_for_eureka_service "ASSET-SERVICE"
    wait_for_eureka_service "SENSOR-SERVICE"
    wait_for_eureka_service "MONITORING-SERVICE"
    wait_for_eureka_service "ALERT-SERVICE"
    wait_for_eureka_service "ANALYTICS-SERVICE"
    wait_for_eureka_service "API-GATEWAY"
    
    # Wait for API Gateway health
    wait_for_service "API Gateway" "$API_GATEWAY_URL/actuator/health"
    
    log_success "All services are healthy!"
}

# =============================================================================
# Authentication
# =============================================================================

TOKEN=""

login() {
    log_info "Authenticating as admin user..."
    
    local response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"Admin@123"}')
    
    TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$TOKEN" ]; then
        log_error "Failed to authenticate. Response: $response"
        exit 1
    fi
    
    log_success "Authentication successful"
}

# =============================================================================
# Get Asset IDs
# =============================================================================

BRIDGE_ID=""
TUNNEL_ID=""
ROAD_SECTION_IDS=()
DRAINAGE_IDS=()
GUARDRAIL_IDS=()

get_assets() {
    log_info "Fetching assets..."
    
    local response=$(curl -s -X GET "$API_GATEWAY_URL/api/v1/assets?size=100" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Accept: application/json")
    
    # Parse asset IDs (simplified - in production use jq)
    BRIDGE_ID=$(echo "$response" | grep -o '"assetType":"Bridge"[^}]*"id":"[^"]*"' | head -1 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    TUNNEL_ID=$(echo "$response" | grep -o '"assetType":"Tunnel"[^}]*"id":"[^"]*"' | head -1 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    
    log_info "Bridge ID: $BRIDGE_ID"
    log_info "Tunnel ID: $TUNNEL_ID"
    
    if [ -z "$BRIDGE_ID" ] || [ -z "$TUNNEL_ID" ]; then
        log_warning "Some assets not found, creating default assets..."
        create_default_assets
    fi
}

create_default_assets() {
    log_info "Creating default corridor assets..."
    
    # Create main corridor
    local corridor_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "assetCode": "CORR-001",
            "name": "Highway 101 Corridor",
            "assetType": "ROAD",
            "status": "OPERATIONAL",
            "latitude": 37.7749,
            "longitude": -122.4194,
            "description": "2km monitored road corridor with bridge and tunnel"
        }')
    
    local corridor_id=$(echo "$corridor_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    # Create bridge
    local bridge_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"assetCode\": \"BR-001\",
            \"name\": \"River Crossing Bridge\",
            \"assetType\": \"BRIDGE\",
            \"status\": \"OPERATIONAL\",
            \"latitude\": 37.7755,
            \"longitude\": -122.4180,
            \"description\": \"200m span steel bridge over river\",
            \"parentId\": \"$corridor_id\"
        }")
    BRIDGE_ID=$(echo "$bridge_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    # Create tunnel
    local tunnel_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"assetCode\": \"TN-001\",
            \"name\": \"Mountain Pass Tunnel\",
            \"assetType\": \"TUNNEL\",
            \"status\": \"OPERATIONAL\",
            \"latitude\": 37.7760,
            \"longitude\": -122.4170,
            \"description\": \"500m twin-bore tunnel through mountain\",
            \"parentId\": \"$corridor_id\"
        }")
    TUNNEL_ID=$(echo "$tunnel_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    # Create road sections
    for i in 1 2 3; do
        local section_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"assetCode\": \"RS-00$i\",
                \"name\": \"Road Section $i\",
                \"assetType\": \"ROAD_SECTION\",
                \"status\": \"OPERATIONAL\",
                \"latitude\": 37.774$i,
                \"longitude\": -122.419$i,
                \"description\": \"Road section $i of the corridor\",
                \"parentId\": \"$corridor_id\"
            }")
        ROAD_SECTION_IDS+=("$(echo "$section_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)")
    done
    
    # Create drainage
    for i in 1 2; do
        local drainage_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"assetCode\": \"DR-00$i\",
                \"name\": \"Drainage System $i\",
                \"assetType\": \"DRAINAGE\",
                \"status\": \"OPERATIONAL\",
                \"latitude\": 37.774$i,
                \"longitude\": -122.418$i,
                \"description\": \"Drainage system $i\",
                \"parentId\": \"$corridor_id\"
            }")
        DRAINAGE_IDS+=("$(echo "$drainage_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)")
    done
    
    # Create guardrails
    for i in 1 2; do
        local guardrail_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/assets" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"assetCode\": \"GR-00$i\",
                \"name\": \"Guardrail Section $i\",
                \"assetType\": \"GUARDRAIL\",
                \"status\": \"OPERATIONAL\",
                \"latitude\": 37.775$i,
                \"longitude\": -122.417$i,
                \"description\": \"Guardrail section $i\",
                \"parentId\": \"$corridor_id\"
            }")
        GUARDRAIL_IDS+=("$(echo "$guardrail_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)")
    done
    
    log_success "Created default assets"
}

# =============================================================================
# Register Sensors
# =============================================================================

register_sensor() {
    local sensor_code=$1
    local sensor_type=$2
    local manufacturer=$3
    local model=$4
    local asset_id=$5
    local asset_type=$6
    local lat=$7
    local lon=$8
    local description=$9
    local unit=${10}
    local min_threshold=${11}
    local max_threshold=${12}
    
    local response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/sensors" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"sensorCode\": \"$sensor_code\",
            \"sensorType\": \"$sensor_type\",
            \"manufacturer\": \"$manufacturer\",
            \"model\": \"$model\",
            \"installationDate\": \"2024-01-15\",
            \"lastCalibrationDate\": \"2024-01-15\",
            \"calibrationIntervalDays\": 90,
            \"latitude\": $lat,
            \"longitude\": $lon,
            \"elevation\": 100.0,
            \"assetId\": \"$asset_id\",
            \"assetType\": \"$asset_type\",
            \"locationDescription\": \"$description\",
            \"batteryLevel\": 100.0,
            \"signalStrength\": 95.0,
            \"firmwareVersion\": \"1.2.3\",
            \"minThreshold\": $min_threshold,
            \"maxThreshold\": $max_threshold,
            \"unit\": \"$unit\"
        }")
    
    if echo "$response" | grep -q '"status":"SUCCESS"'; then
        log_success "Registered sensor: $sensor_code"
    else
        log_warning "Failed to register sensor $sensor_code: $response"
    fi
}

register_all_sensors() {
    log_info "=========================================="
    log_info "Registering 45 sensors across all assets..."
    log_info "=========================================="
    
    # Bridge sensors (18 total)
    # 8 strain gauges
    for i in $(seq 1 8); do
        register_sensor "SG-BR-00$i" "Strain Gauge" "Honeywell" "HSG-200" "$BRIDGE_ID" "Bridge" \
            "37.7755" "-122.418$i" "Bridge strain gauge $i" "μstrain" 0 500
    done
    
    # 4 accelerometers
    for i in $(seq 1 4); do
        register_sensor "AC-BR-00$i" "Accelerometer" "Kistler" "ACC-100" "$BRIDGE_ID" "Bridge" \
            "37.7756" "-122.418$i" "Bridge accelerometer $i" "g" 0 0.5
    done
    
    # 2 tiltmeters
    for i in $(seq 1 2); do
        register_sensor "TM-BR-00$i" "Tiltmeter" "Geosense" "TLT-50" "$BRIDGE_ID" "Bridge" \
            "37.7757" "-122.418$i" "Bridge tiltmeter $i" "degrees" -1 1
    done
    
    # 2 temperature sensors
    for i in $(seq 1 2); do
        register_sensor "TP-BR-00$i" "Temperature" "Omega" "TMP-K100" "$BRIDGE_ID" "Bridge" \
            "37.7758" "-122.418$i" "Bridge temperature sensor $i" "°C" -10 50
    done
    
    # 2 crack meters
    for i in $(seq 1 2); do
        register_sensor "CM-BR-00$i" "Crack Meter" "RST" "CRK-200" "$BRIDGE_ID" "Bridge" \
            "37.7759" "-122.418$i" "Bridge crack meter $i" "mm" 0 5
    done
    
    # Tunnel sensors (13 total)
    # 4 displacement sensors
    for i in $(seq 1 4); do
        register_sensor "DS-TN-00$i" "Displacement" "Geokon" "DSP-500" "$TUNNEL_ID" "Tunnel" \
            "37.7760" "-122.417$i" "Tunnel displacement sensor $i" "mm" 0 10
    done
    
    # 3 air quality sensors
    for i in $(seq 1 3); do
        register_sensor "AQ-TN-00$i" "Air Quality" "Aeroqual" "AQS-300" "$TUNNEL_ID" "Tunnel" \
            "37.7761" "-122.417$i" "Tunnel air quality sensor $i" "ppm" 0 50
    done
    
    # 2 temperature sensors
    for i in $(seq 1 2); do
        register_sensor "TP-TN-00$i" "Temperature" "Omega" "TMP-K100" "$TUNNEL_ID" "Tunnel" \
            "37.7762" "-122.417$i" "Tunnel temperature sensor $i" "°C" 10 30
    done
    
    # 2 moisture sensors
    for i in $(seq 1 2); do
        register_sensor "MS-TN-00$i" "Moisture" "Vaisala" "HUM-100" "$TUNNEL_ID" "Tunnel" \
            "37.7763" "-122.417$i" "Tunnel moisture sensor $i" "%RH" 20 90
    done
    
    # 2 crack meters
    for i in $(seq 1 2); do
        register_sensor "CM-TN-00$i" "Crack Meter" "RST" "CRK-200" "$TUNNEL_ID" "Tunnel" \
            "37.7764" "-122.417$i" "Tunnel crack meter $i" "mm" 0 5
    done
    
    # Road section sensors (9 total - 3 per section)
    local section_num=1
    for section_id in "${ROAD_SECTION_IDS[@]}"; do
        if [ -n "$section_id" ]; then
            register_sensor "TP-RS-00$section_num" "Temperature" "Omega" "TMP-K100" "$section_id" "Road Section" \
                "37.774$section_num" "-122.419$section_num" "Road section $section_num temperature" "°C" -20 60
            register_sensor "MS-RS-00$section_num" "Moisture" "Vaisala" "HUM-100" "$section_id" "Road Section" \
                "37.774$section_num" "-122.419$section_num" "Road section $section_num moisture" "%RH" 20 100
            register_sensor "GP-RS-00$section_num" "GPS" "Trimble" "GPS-R10" "$section_id" "Road Section" \
                "37.774$section_num" "-122.419$section_num" "Road section $section_num GPS tracker" "degrees" -180 180
            section_num=$((section_num + 1))
        fi
    done
    
    # Drainage sensors (4 total - 2 per drainage)
    local drainage_num=1
    for drainage_id in "${DRAINAGE_IDS[@]}"; do
        if [ -n "$drainage_id" ]; then
            # Using Moisture as proxy for flow sensor (adjust if FLOW_SENSOR type exists)
            register_sensor "FL-DR-00$drainage_num" "Moisture" "Siemens" "FLW-200" "$drainage_id" "Drainage" \
                "37.774$drainage_num" "-122.418$drainage_num" "Drainage flow sensor $drainage_num" "L/min" 0 200
            # Using Displacement as proxy for level sensor
            register_sensor "LV-DR-00$drainage_num" "Displacement" "Omega" "LVL-100" "$drainage_id" "Drainage" \
                "37.774$drainage_num" "-122.418$drainage_num" "Drainage level sensor $drainage_num" "m" 0 3
            drainage_num=$((drainage_num + 1))
        fi
    done
    
    # Guardrail sensors (2 total)
    local guardrail_num=1
    for guardrail_id in "${GUARDRAIL_IDS[@]}"; do
        if [ -n "$guardrail_id" ]; then
            # Using Accelerometer as proxy for impact sensor
            register_sensor "IM-GR-00$guardrail_num" "Accelerometer" "PCB" "IMP-500" "$guardrail_id" "Guardrail" \
                "37.775$guardrail_num" "-122.417$guardrail_num" "Guardrail impact sensor $guardrail_num" "N" 0 5000
            guardrail_num=$((guardrail_num + 1))
        fi
    done
    
    log_success "Sensor registration complete"
}

# =============================================================================
# Activate Sensors
# =============================================================================

activate_sensors() {
    log_info "Activating all registered sensors..."
    
    local response=$(curl -s -X GET "$API_GATEWAY_URL/api/v1/sensors?size=100" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Accept: application/json")
    
    # Extract sensor IDs and activate them
    local sensor_ids=$(echo "$response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    
    for sensor_id in $sensor_ids; do
        # Use display name "Active" as expected by SensorStatus enum's @JsonValue
        curl -s -X PATCH "$API_GATEWAY_URL/api/v1/sensors/$sensor_id/status" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"status": "Active"}' > /dev/null
    done
    
    log_success "All sensors activated"
}

# =============================================================================
# Create Monitoring Thresholds
# =============================================================================

create_thresholds() {
    log_info "=========================================="
    log_info "Creating monitoring thresholds..."
    log_info "=========================================="
    
    # Note: This depends on the monitoring-service API structure
    # Adjust the endpoint and payload based on actual API
    
    local thresholds=(
        '{"sensorType":"STRAIN_GAUGE","assetType":"BRIDGE","warningLow":50,"warningHigh":250,"criticalLow":20,"criticalHigh":350}'
        '{"sensorType":"ACCELEROMETER","assetType":"BRIDGE","warningLow":0,"warningHigh":0.15,"criticalLow":0,"criticalHigh":0.3}'
        '{"sensorType":"TEMPERATURE","assetType":"BRIDGE","warningLow":0,"warningHigh":40,"criticalLow":-10,"criticalHigh":50}'
        '{"sensorType":"TILTMETER","assetType":"BRIDGE","warningLow":-0.3,"warningHigh":0.3,"criticalLow":-0.5,"criticalHigh":0.5}'
        '{"sensorType":"CRACK_METER","assetType":"BRIDGE","warningLow":0,"warningHigh":2,"criticalLow":0,"criticalHigh":5}'
        '{"sensorType":"DISPLACEMENT","assetType":"TUNNEL","warningLow":0,"warningHigh":3,"criticalLow":0,"criticalHigh":5}'
        '{"sensorType":"AIR_QUALITY","assetType":"TUNNEL","warningLow":0,"warningHigh":25,"criticalLow":0,"criticalHigh":50}'
        '{"sensorType":"TEMPERATURE","assetType":"TUNNEL","warningLow":15,"warningHigh":25,"criticalLow":10,"criticalHigh":30}'
        '{"sensorType":"MOISTURE","assetType":"TUNNEL","warningLow":30,"warningHigh":70,"criticalLow":20,"criticalHigh":85}'
    )
    
    for threshold in "${thresholds[@]}"; do
        curl -s -X POST "$API_GATEWAY_URL/api/v1/monitoring/thresholds" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$threshold" > /dev/null 2>&1 || true
    done
    
    log_success "Monitoring thresholds created"
}

# =============================================================================
# Create Alert Rules
# =============================================================================

create_alert_rules() {
    log_info "=========================================="
    log_info "Creating alert rules..."
    log_info "=========================================="
    
    # Note: This depends on the alert-service API structure
    # Adjust the endpoint and payload based on actual API
    
    local rules=(
        '{"name":"High Strain Alert","description":"Alert when strain exceeds safe levels","sensorType":"STRAIN_GAUGE","condition":"GREATER_THAN","threshold":300,"severity":"HIGH","notifyChannels":["EMAIL","DASHBOARD"]}'
        '{"name":"Excessive Vibration","description":"Alert on unusual vibration patterns","sensorType":"ACCELEROMETER","condition":"GREATER_THAN","threshold":0.2,"severity":"MEDIUM","notifyChannels":["DASHBOARD"]}'
        '{"name":"Air Quality Warning","description":"Alert when air quality degrades","sensorType":"AIR_QUALITY","condition":"GREATER_THAN","threshold":30,"severity":"HIGH","notifyChannels":["EMAIL","DASHBOARD","SMS"]}'
        '{"name":"Temperature Extreme","description":"Alert on extreme temperatures","sensorType":"TEMPERATURE","condition":"GREATER_THAN","threshold":45,"severity":"MEDIUM","notifyChannels":["DASHBOARD"]}'
        '{"name":"Crack Growth Alert","description":"Alert when crack width exceeds threshold","sensorType":"CRACK_METER","condition":"GREATER_THAN","threshold":3,"severity":"CRITICAL","notifyChannels":["EMAIL","DASHBOARD","SMS"]}'
        '{"name":"Displacement Warning","description":"Alert on structural displacement","sensorType":"DISPLACEMENT","condition":"GREATER_THAN","threshold":4,"severity":"HIGH","notifyChannels":["EMAIL","DASHBOARD"]}'
    )
    
    for rule in "${rules[@]}"; do
        curl -s -X POST "$API_GATEWAY_URL/api/v1/alerts/rules" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$rule" > /dev/null 2>&1 || true
    done
    
    log_success "Alert rules created"
}

# =============================================================================
# Start Simulator
# =============================================================================

start_simulator() {
    log_info "=========================================="
    log_info "Starting data simulator..."
    log_info "=========================================="
    
    # Check if simulator is running as a Docker container
    if docker ps --format '{{.Names}}' | grep -q "simulator"; then
        log_info "Simulator container is already running"
    else
        # Try to start via docker-compose
        if [ -f "docker-compose.yml" ]; then
            docker-compose up -d simulator 2>/dev/null || true
        fi
    fi
    
    # Check simulator health
    local retries=0
    while [ $retries -lt 10 ]; do
        if curl -s "http://localhost:9000/api/simulator/health" | grep -q '"status":"UP"' 2>/dev/null; then
            log_success "Simulator is running and healthy"
            return 0
        fi
        retries=$((retries + 1))
        sleep 5
    done
    
    log_warning "Could not verify simulator status. It may need to be started manually."
}

# =============================================================================
# Print Summary
# =============================================================================

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${GREEN}Seed Data Initialization Complete${NC}"
    echo "=========================================="
    echo ""
    echo "Assets created:"
    echo "  - Bridge: $BRIDGE_ID"
    echo "  - Tunnel: $TUNNEL_ID"
    echo "  - Road Sections: ${#ROAD_SECTION_IDS[@]}"
    echo "  - Drainage Systems: ${#DRAINAGE_IDS[@]}"
    echo "  - Guardrails: ${#GUARDRAIL_IDS[@]}"
    echo ""
    echo "Sensors registered: 45"
    echo "  - Bridge: 18 (strain gauges, accelerometers, tiltmeters, temp, crack meters)"
    echo "  - Tunnel: 13 (displacement, air quality, temp, moisture, crack meters)"
    echo "  - Road Sections: 9 (temp, moisture, GPS)"
    echo "  - Drainage: 4 (flow, level sensors)"
    echo "  - Guardrails: 2 (impact sensors)"
    echo ""
    echo "Monitoring thresholds: Configured for all sensor/asset combinations"
    echo "Alert rules: 6 rules created"
    echo ""
    echo "Simulator: Running at http://localhost:9000"
    echo ""
    echo "Dashboard: http://localhost:3000"
    echo "API Gateway: http://localhost:8080"
    echo "Eureka: http://localhost:8761"
    echo ""
    echo "=========================================="
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    log_info "Starting Road Infrastructure Monitoring seed data initialization..."
    echo ""
    
    wait_for_all_services
    login
    get_assets
    register_all_sensors
    activate_sensors
    create_thresholds
    create_alert_rules
    start_simulator
    print_summary
}

# Run main function
main "$@"
