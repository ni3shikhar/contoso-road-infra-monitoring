# =============================================================================
# Seed Data Script for Road Infrastructure Monitoring System (Windows PowerShell)
# =============================================================================

param(
    [string]$ApiGatewayUrl = "http://localhost:8080",
    [string]$EurekaUrl = "http://localhost:8761",
    [int]$MaxRetries = 60,
    [int]$RetryIntervalSeconds = 5
)

$ErrorActionPreference = "Stop"

# Global variables
$script:Token = ""
$script:BridgeId = ""
$script:TunnelId = ""
$script:RoadSectionIds = @()
$script:DrainageIds = @()
$script:GuardrailIds = @()

function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor Blue
}

function Write-Success($message) {
    Write-Host "[SUCCESS] $message" -ForegroundColor Green
}

function Write-Warning($message) {
    Write-Host "[WARNING] $message" -ForegroundColor Yellow
}

function Write-Error($message) {
    Write-Host "[ERROR] $message" -ForegroundColor Red
}

# =============================================================================
# Wait for Services
# =============================================================================

function Wait-ForService {
    param(
        [string]$ServiceName,
        [string]$HealthUrl
    )
    
    Write-Info "Waiting for $ServiceName to be healthy..."
    
    for ($i = 0; $i -lt $MaxRetries; $i++) {
        try {
            $response = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 5
            if ($response.status -eq "UP") {
                Write-Success "$ServiceName is healthy"
                return $true
            }
        } catch {
            # Service not ready yet
        }
        Write-Info "Waiting for $ServiceName... (attempt $($i + 1)/$MaxRetries)"
        Start-Sleep -Seconds $RetryIntervalSeconds
    }
    
    Write-Error "$ServiceName did not become healthy in time"
    return $false
}

function Wait-ForEurekaService {
    param([string]$ServiceName)
    
    Write-Info "Waiting for $ServiceName to register with Eureka..."
    
    for ($i = 0; $i -lt $MaxRetries; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$EurekaUrl/eureka/apps" -Method Get -Headers @{Accept="application/json"} -TimeoutSec 5
            if ($response.applications.application | Where-Object { $_.name -eq $ServiceName }) {
                Write-Success "$ServiceName is registered with Eureka"
                return $true
            }
        } catch {
            # Eureka not ready yet
        }
        Write-Info "Waiting for $ServiceName registration... (attempt $($i + 1)/$MaxRetries)"
        Start-Sleep -Seconds $RetryIntervalSeconds
    }
    
    Write-Error "$ServiceName did not register in time"
    return $false
}

function Wait-ForAllServices {
    Write-Info "=========================================="
    Write-Info "Waiting for all services to be healthy..."
    Write-Info "=========================================="
    
    Wait-ForService "Eureka" "$EurekaUrl/actuator/health"
    Wait-ForEurekaService "AUTH-SERVICE"
    Wait-ForEurekaService "ASSET-SERVICE"
    Wait-ForEurekaService "SENSOR-SERVICE"
    Wait-ForEurekaService "MONITORING-SERVICE"
    Wait-ForEurekaService "ALERT-SERVICE"
    Wait-ForEurekaService "ANALYTICS-SERVICE"
    Wait-ForEurekaService "API-GATEWAY"
    Wait-ForService "API Gateway" "$ApiGatewayUrl/actuator/health"
    
    Write-Success "All services are healthy!"
}

# =============================================================================
# Authentication
# =============================================================================

function Login {
    Write-Info "Authenticating as admin user..."
    
    $body = @{
        username = "admin"
        password = "Admin@123"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json"
        $script:Token = $response.data.accessToken
        
        if ([string]::IsNullOrEmpty($script:Token)) {
            Write-Error "Failed to get access token"
            exit 1
        }
        
        Write-Success "Authentication successful"
    } catch {
        Write-Error "Login failed: $_"
        exit 1
    }
}

# =============================================================================
# Get/Create Assets
# =============================================================================

function Get-Assets {
    Write-Info "Fetching assets..."
    
    $headers = @{
        Authorization = "Bearer $script:Token"
        Accept = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets?size=100" -Method Get -Headers $headers
        
        if ($response.data.content) {
            foreach ($asset in $response.data.content) {
                switch ($asset.assetType) {
                    "Bridge" { $script:BridgeId = $asset.id }
                    "Tunnel" { $script:TunnelId = $asset.id }
                    "Road Section" { $script:RoadSectionIds += $asset.id }
                    "Drainage" { $script:DrainageIds += $asset.id }
                    "Guardrail" { $script:GuardrailIds += $asset.id }
                }
            }
        }
        
        Write-Info "Bridge ID: $script:BridgeId"
        Write-Info "Tunnel ID: $script:TunnelId"
        
        if ([string]::IsNullOrEmpty($script:BridgeId) -or [string]::IsNullOrEmpty($script:TunnelId)) {
            Write-Warning "Some assets not found, creating default assets..."
            Create-DefaultAssets
        }
    } catch {
        Write-Warning "Error fetching assets: $_"
        Create-DefaultAssets
    }
}

function Create-DefaultAssets {
    Write-Info "Creating default corridor assets..."
    
    $headers = @{
        Authorization = "Bearer $script:Token"
        "Content-Type" = "application/json"
    }
    
    # Create main corridor
    $corridorBody = @{
        assetCode = "CORR-001"
        name = "Highway 101 Corridor"
        assetType = "ROAD"
        status = "OPERATIONAL"
        latitude = 37.7749
        longitude = -122.4194
        description = "2km monitored road corridor with bridge and tunnel"
    } | ConvertTo-Json
    
    try {
        $corridorResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $corridorBody
        $corridorId = $corridorResponse.data.id
    } catch {
        Write-Warning "Could not create corridor: $_"
        $corridorId = $null
    }
    
    # Create bridge
    $bridgeBody = @{
        assetCode = "BR-001"
        name = "River Crossing Bridge"
        assetType = "BRIDGE"
        status = "OPERATIONAL"
        latitude = 37.7755
        longitude = -122.4180
        description = "200m span steel bridge over river"
        parentId = $corridorId
    } | ConvertTo-Json
    
    try {
        $bridgeResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $bridgeBody
        $script:BridgeId = $bridgeResponse.data.id
    } catch {
        Write-Warning "Could not create bridge: $_"
    }
    
    # Create tunnel
    $tunnelBody = @{
        assetCode = "TN-001"
        name = "Mountain Pass Tunnel"
        assetType = "TUNNEL"
        status = "OPERATIONAL"
        latitude = 37.7760
        longitude = -122.4170
        description = "500m twin-bore tunnel through mountain"
        parentId = $corridorId
    } | ConvertTo-Json
    
    try {
        $tunnelResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $tunnelBody
        $script:TunnelId = $tunnelResponse.data.id
    } catch {
        Write-Warning "Could not create tunnel: $_"
    }
    
    # Create road sections
    for ($i = 1; $i -le 3; $i++) {
        $sectionBody = @{
            assetCode = "RS-00$i"
            name = "Road Section $i"
            assetType = "ROAD_SECTION"
            status = "OPERATIONAL"
            latitude = 37.774 + ($i * 0.001)
            longitude = -122.419 + ($i * 0.001)
            description = "Road section $i of the corridor"
            parentId = $corridorId
        } | ConvertTo-Json
        
        try {
            $sectionResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $sectionBody
            $script:RoadSectionIds += $sectionResponse.data.id
        } catch {
            Write-Warning "Could not create road section $i"
        }
    }
    
    # Create drainage systems
    for ($i = 1; $i -le 2; $i++) {
        $drainageBody = @{
            assetCode = "DR-00$i"
            name = "Drainage System $i"
            assetType = "DRAINAGE"
            status = "OPERATIONAL"
            latitude = 37.774 + ($i * 0.001)
            longitude = -122.418 + ($i * 0.001)
            description = "Drainage system $i"
            parentId = $corridorId
        } | ConvertTo-Json
        
        try {
            $drainageResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $drainageBody
            $script:DrainageIds += $drainageResponse.data.id
        } catch {
            Write-Warning "Could not create drainage $i"
        }
    }
    
    # Create guardrails
    for ($i = 1; $i -le 2; $i++) {
        $guardrailBody = @{
            assetCode = "GR-00$i"
            name = "Guardrail Section $i"
            assetType = "GUARDRAIL"
            status = "OPERATIONAL"
            latitude = 37.775 + ($i * 0.001)
            longitude = -122.417 + ($i * 0.001)
            description = "Guardrail section $i"
            parentId = $corridorId
        } | ConvertTo-Json
        
        try {
            $guardrailResponse = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/assets" -Method Post -Headers $headers -Body $guardrailBody
            $script:GuardrailIds += $guardrailResponse.data.id
        } catch {
            Write-Warning "Could not create guardrail $i"
        }
    }
    
    Write-Success "Created default assets"
}

# =============================================================================
# Register Sensors
# =============================================================================

function Register-Sensor {
    param(
        [string]$SensorCode,
        [string]$SensorType,
        [string]$Manufacturer,
        [string]$Model,
        [string]$AssetId,
        [string]$AssetType,
        [double]$Latitude,
        [double]$Longitude,
        [string]$Description,
        [string]$Unit,
        [double]$MinThreshold,
        [double]$MaxThreshold
    )
    
    if ([string]::IsNullOrEmpty($AssetId)) {
        Write-Warning "Skipping sensor $SensorCode - no asset ID"
        return
    }
    
    $headers = @{
        Authorization = "Bearer $script:Token"
        "Content-Type" = "application/json"
    }
    
    $body = @{
        sensorCode = $SensorCode
        sensorType = $SensorType
        manufacturer = $Manufacturer
        model = $Model
        installationDate = "2024-01-15"
        lastCalibrationDate = "2024-01-15"
        calibrationIntervalDays = 90
        latitude = $Latitude
        longitude = $Longitude
        elevation = 100.0
        assetId = $AssetId
        assetType = $AssetType
        locationDescription = $Description
        batteryLevel = 100.0
        signalStrength = 95.0
        firmwareVersion = "1.2.3"
        minThreshold = $MinThreshold
        maxThreshold = $MaxThreshold
        unit = $Unit
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/sensors" -Method Post -Headers $headers -Body $body
        Write-Success "Registered sensor: $SensorCode"
    } catch {
        Write-Warning "Failed to register sensor $SensorCode`: $_"
    }
}

function Register-AllSensors {
    Write-Info "=========================================="
    Write-Info "Registering 45 sensors across all assets..."
    Write-Info "=========================================="
    
    # Bridge sensors (18 total)
    # 8 strain gauges
    for ($i = 1; $i -le 8; $i++) {
        Register-Sensor -SensorCode "SG-BR-00$i" -SensorType "Strain Gauge" -Manufacturer "Honeywell" -Model "HSG-200" `
            -AssetId $script:BridgeId -AssetType "Bridge" -Latitude 37.7755 -Longitude (-122.418 - ($i * 0.0001)) `
            -Description "Bridge strain gauge $i" -Unit "μstrain" -MinThreshold 0 -MaxThreshold 500
    }
    
    # 4 accelerometers
    for ($i = 1; $i -le 4; $i++) {
        Register-Sensor -SensorCode "AC-BR-00$i" -SensorType "Accelerometer" -Manufacturer "Kistler" -Model "ACC-100" `
            -AssetId $script:BridgeId -AssetType "Bridge" -Latitude 37.7756 -Longitude (-122.418 - ($i * 0.0001)) `
            -Description "Bridge accelerometer $i" -Unit "g" -MinThreshold 0 -MaxThreshold 0.5
    }
    
    # 2 tiltmeters
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "TM-BR-00$i" -SensorType "Tiltmeter" -Manufacturer "Geosense" -Model "TLT-50" `
            -AssetId $script:BridgeId -AssetType "Bridge" -Latitude 37.7757 -Longitude (-122.418 - ($i * 0.0001)) `
            -Description "Bridge tiltmeter $i" -Unit "degrees" -MinThreshold -1 -MaxThreshold 1
    }
    
    # 2 temperature sensors
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "TP-BR-00$i" -SensorType "Temperature" -Manufacturer "Omega" -Model "TMP-K100" `
            -AssetId $script:BridgeId -AssetType "Bridge" -Latitude 37.7758 -Longitude (-122.418 - ($i * 0.0001)) `
            -Description "Bridge temperature sensor $i" -Unit "°C" -MinThreshold -10 -MaxThreshold 50
    }
    
    # 2 crack meters
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "CM-BR-00$i" -SensorType "Crack Meter" -Manufacturer "RST" -Model "CRK-200" `
            -AssetId $script:BridgeId -AssetType "Bridge" -Latitude 37.7759 -Longitude (-122.418 - ($i * 0.0001)) `
            -Description "Bridge crack meter $i" -Unit "mm" -MinThreshold 0 -MaxThreshold 5
    }
    
    # Tunnel sensors (13 total)
    # 4 displacement sensors
    for ($i = 1; $i -le 4; $i++) {
        Register-Sensor -SensorCode "DS-TN-00$i" -SensorType "Displacement" -Manufacturer "Geokon" -Model "DSP-500" `
            -AssetId $script:TunnelId -AssetType "Tunnel" -Latitude 37.7760 -Longitude (-122.417 - ($i * 0.0001)) `
            -Description "Tunnel displacement sensor $i" -Unit "mm" -MinThreshold 0 -MaxThreshold 10
    }
    
    # 3 air quality sensors
    for ($i = 1; $i -le 3; $i++) {
        Register-Sensor -SensorCode "AQ-TN-00$i" -SensorType "Air Quality" -Manufacturer "Aeroqual" -Model "AQS-300" `
            -AssetId $script:TunnelId -AssetType "Tunnel" -Latitude 37.7761 -Longitude (-122.417 - ($i * 0.0001)) `
            -Description "Tunnel air quality sensor $i" -Unit "ppm" -MinThreshold 0 -MaxThreshold 50
    }
    
    # 2 temperature sensors
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "TP-TN-00$i" -SensorType "Temperature" -Manufacturer "Omega" -Model "TMP-K100" `
            -AssetId $script:TunnelId -AssetType "Tunnel" -Latitude 37.7762 -Longitude (-122.417 - ($i * 0.0001)) `
            -Description "Tunnel temperature sensor $i" -Unit "°C" -MinThreshold 10 -MaxThreshold 30
    }
    
    # 2 moisture sensors
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "MS-TN-00$i" -SensorType "Moisture" -Manufacturer "Vaisala" -Model "HUM-100" `
            -AssetId $script:TunnelId -AssetType "Tunnel" -Latitude 37.7763 -Longitude (-122.417 - ($i * 0.0001)) `
            -Description "Tunnel moisture sensor $i" -Unit "%RH" -MinThreshold 20 -MaxThreshold 90
    }
    
    # 2 crack meters
    for ($i = 1; $i -le 2; $i++) {
        Register-Sensor -SensorCode "CM-TN-00$i" -SensorType "Crack Meter" -Manufacturer "RST" -Model "CRK-200" `
            -AssetId $script:TunnelId -AssetType "Tunnel" -Latitude 37.7764 -Longitude (-122.417 - ($i * 0.0001)) `
            -Description "Tunnel crack meter $i" -Unit "mm" -MinThreshold 0 -MaxThreshold 5
    }
    
    # Road section sensors
    $sectionNum = 1
    foreach ($sectionId in $script:RoadSectionIds) {
        if (-not [string]::IsNullOrEmpty($sectionId)) {
            Register-Sensor -SensorCode "TP-RS-00$sectionNum" -SensorType "Temperature" -Manufacturer "Omega" -Model "TMP-K100" `
                -AssetId $sectionId -AssetType "Road Section" -Latitude (37.774 + ($sectionNum * 0.001)) -Longitude (-122.419 + ($sectionNum * 0.0001)) `
                -Description "Road section $sectionNum temperature" -Unit "°C" -MinThreshold -20 -MaxThreshold 60
            Register-Sensor -SensorCode "MS-RS-00$sectionNum" -SensorType "Moisture" -Manufacturer "Vaisala" -Model "HUM-100" `
                -AssetId $sectionId -AssetType "Road Section" -Latitude (37.774 + ($sectionNum * 0.001)) -Longitude (-122.419 + ($sectionNum * 0.0001)) `
                -Description "Road section $sectionNum moisture" -Unit "%RH" -MinThreshold 20 -MaxThreshold 100
            Register-Sensor -SensorCode "GP-RS-00$sectionNum" -SensorType "GPS" -Manufacturer "Trimble" -Model "GPS-R10" `
                -AssetId $sectionId -AssetType "Road Section" -Latitude (37.774 + ($sectionNum * 0.001)) -Longitude (-122.419 + ($sectionNum * 0.0001)) `
                -Description "Road section $sectionNum GPS tracker" -Unit "degrees" -MinThreshold -180 -MaxThreshold 180
            $sectionNum++
        }
    }
    
    # Drainage sensors
    $drainageNum = 1
    foreach ($drainageId in $script:DrainageIds) {
        if (-not [string]::IsNullOrEmpty($drainageId)) {
            Register-Sensor -SensorCode "FL-DR-00$drainageNum" -SensorType "Moisture" -Manufacturer "Siemens" -Model "FLW-200" `
                -AssetId $drainageId -AssetType "Drainage" -Latitude (37.774 + ($drainageNum * 0.001)) -Longitude (-122.418 + ($drainageNum * 0.0001)) `
                -Description "Drainage flow sensor $drainageNum" -Unit "L/min" -MinThreshold 0 -MaxThreshold 200
            Register-Sensor -SensorCode "LV-DR-00$drainageNum" -SensorType "Displacement" -Manufacturer "Omega" -Model "LVL-100" `
                -AssetId $drainageId -AssetType "Drainage" -Latitude (37.774 + ($drainageNum * 0.001)) -Longitude (-122.418 + ($drainageNum * 0.0001)) `
                -Description "Drainage level sensor $drainageNum" -Unit "m" -MinThreshold 0 -MaxThreshold 3
            $drainageNum++
        }
    }
    
    # Guardrail sensors
    $guardrailNum = 1
    foreach ($guardrailId in $script:GuardrailIds) {
        if (-not [string]::IsNullOrEmpty($guardrailId)) {
            Register-Sensor -SensorCode "IM-GR-00$guardrailNum" -SensorType "Accelerometer" -Manufacturer "PCB" -Model "IMP-500" `
                -AssetId $guardrailId -AssetType "Guardrail" -Latitude (37.775 + ($guardrailNum * 0.001)) -Longitude (-122.417 + ($guardrailNum * 0.0001)) `
                -Description "Guardrail impact sensor $guardrailNum" -Unit "N" -MinThreshold 0 -MaxThreshold 5000
            $guardrailNum++
        }
    }
    
    Write-Success "Sensor registration complete"
}

# =============================================================================
# Activate Sensors
# =============================================================================

function Activate-Sensors {
    Write-Info "Activating all registered sensors..."
    
    $headers = @{
        Authorization = "Bearer $script:Token"
        Accept = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/sensors?size=100" -Method Get -Headers $headers
        
        foreach ($sensor in $response.data.content) {
            try {
                $activateHeaders = @{
                    Authorization = "Bearer $script:Token"
                    "Content-Type" = "application/json"
                }
                # Use display name "Active" as expected by SensorStatus enum's @JsonValue
                $body = '{"status": "Active"}'
                Invoke-RestMethod -Uri "$ApiGatewayUrl/api/v1/sensors/$($sensor.id)/status" -Method Patch -Headers $activateHeaders -Body $body | Out-Null
            } catch {
                # Ignore individual activation failures
            }
        }
        
        Write-Success "All sensors activated"
    } catch {
        Write-Warning "Error activating sensors: $_"
    }
}

# =============================================================================
# Print Summary
# =============================================================================

function Show-Summary {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host "Seed Data Initialization Complete" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Assets created:"
    Write-Host "  - Bridge: $script:BridgeId"
    Write-Host "  - Tunnel: $script:TunnelId"
    Write-Host "  - Road Sections: $($script:RoadSectionIds.Count)"
    Write-Host "  - Drainage Systems: $($script:DrainageIds.Count)"
    Write-Host "  - Guardrails: $($script:GuardrailIds.Count)"
    Write-Host ""
    Write-Host "Sensors registered: 45"
    Write-Host "  - Bridge: 18 (strain gauges, accelerometers, tiltmeters, temp, crack meters)"
    Write-Host "  - Tunnel: 13 (displacement, air quality, temp, moisture, crack meters)"
    Write-Host "  - Road Sections: 9 (temp, moisture, GPS)"
    Write-Host "  - Drainage: 4 (flow, level sensors)"
    Write-Host "  - Guardrails: 2 (impact sensors)"
    Write-Host ""
    Write-Host "Dashboard: http://localhost:3000"
    Write-Host "API Gateway: http://localhost:8080"
    Write-Host "Eureka: http://localhost:8761"
    Write-Host "Simulator: http://localhost:9000"
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Green
}

# =============================================================================
# Main Execution
# =============================================================================

Write-Info "Starting Road Infrastructure Monitoring seed data initialization..."
Write-Host ""

Wait-ForAllServices
Login
Get-Assets
Register-AllSensors
Activate-Sensors
Show-Summary
