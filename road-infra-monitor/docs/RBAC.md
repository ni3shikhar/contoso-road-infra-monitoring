# Road Infrastructure Monitoring - Role-Based Access Control (RBAC)

## Overview

The Road Infrastructure Monitoring system implements a comprehensive Role-Based Access Control (RBAC) system with four predefined roles and granular permissions. This document details the security model and its mapping to real-world personas.

## Roles

### ADMIN
**Full System Administrator**

Administrators have unrestricted access to all system functionality. This role should be limited to IT administrators and system managers responsible for overall system configuration and user management.

### ENGINEER
**Technical Specialist**

Engineers have full read/write access to technical resources including sensors, assets, and analytics. They can configure thresholds, manage alert rules, and perform data exports.

### OPERATOR
**Field Operations Personnel**

Operators have day-to-day operational access focused on monitoring, incident response, and inspections. They cannot modify system configurations but can manage alerts and update asset progress.

### VIEWER
**Read-Only Stakeholders**

Viewers have read-only access to all resources for reporting and oversight purposes. They cannot modify any data or acknowledge alerts.

---

## Permissions

### Sensor Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| SENSOR_READ | View sensor data and readings | ✓ | ✓ | ✓ | ✓ |
| SENSOR_WRITE | Create and update sensors | ✓ | ✓ | | |
| SENSOR_DELETE | Delete sensors | ✓ | | | |
| SENSOR_CONFIGURE | Configure sensor settings, calibration | ✓ | ✓ | | |

### Asset Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| ASSET_READ | View asset details and status | ✓ | ✓ | ✓ | ✓ |
| ASSET_WRITE | Create and update assets | ✓ | ✓ | | |
| ASSET_DELETE | Delete assets | ✓ | | | |
| ASSET_PROGRESS_UPDATE | Update construction/maintenance progress | ✓ | ✓ | ✓ | |

### Monitoring Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| MONITORING_READ | View health status and metrics | ✓ | ✓ | ✓ | ✓ |
| MONITORING_CONFIGURE_THRESHOLDS | Set monitoring thresholds | ✓ | ✓ | | |

### Alert Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| ALERT_READ | View alerts and history | ✓ | ✓ | ✓ | ✓ |
| ALERT_ACKNOWLEDGE | Acknowledge received alerts | ✓ | ✓ | ✓ | |
| ALERT_ASSIGN | Assign alerts to team members | ✓ | ✓ | ✓ | |
| ALERT_RESOLVE | Mark alerts as resolved | ✓ | ✓ | ✓ | |
| ALERT_RULE_MANAGE | Create and modify alert rules | ✓ | ✓ | | |

### Analytics Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| ANALYTICS_READ | View KPIs and reports | ✓ | ✓ | ✓ | ✓ |
| ANALYTICS_EXPORT | Export data to external formats | ✓ | ✓ | | |
| ANALYTICS_REFRESH | Manually trigger KPI recalculation | ✓ | ✓ | | |

### Inspection Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| INSPECTION_READ | View inspection records | ✓ | ✓ | ✓ | ✓ |
| INSPECTION_WRITE | Create and update inspections | ✓ | ✓ | ✓ | |

### Administration Permissions
| Permission | Description | ADMIN | ENGINEER | OPERATOR | VIEWER |
|------------|-------------|:-----:|:--------:|:--------:|:------:|
| USER_READ | View user accounts | ✓ | | | |
| USER_MANAGE | Create, update, delete users | ✓ | | | |
| SYSTEM_ADMIN | Full system configuration | ✓ | | | |

---

## Persona-to-Role Mapping

This section maps real-world job titles and responsibilities to system roles.

### Site/Project Manager → OPERATOR
**Responsibilities:**
- Track construction and maintenance progress
- Manage field crew schedules
- Respond to alerts and incidents
- Document inspections

**Granted Capabilities:**
- View all sensor data and asset information
- Update asset progress status
- Acknowledge and resolve alerts
- Create inspection records

### Structural/Civil Engineer → ENGINEER
**Responsibilities:**
- Analyze structural health data
- Configure monitoring thresholds
- Review sensor calibration
- Assess infrastructure integrity

**Granted Capabilities:**
- Full sensor management (except deletion)
- Configure monitoring parameters
- Manage alert rules
- Export data for detailed analysis

### Maintenance/Operations Manager → OPERATOR
**Responsibilities:**
- Plan maintenance schedules
- Coordinate repair crews
- Monitor asset health status
- Respond to degradation alerts

**Granted Capabilities:**
- View real-time health data
- Update maintenance progress
- Manage alert assignments
- Document maintenance inspections

### Safety Officer → OPERATOR
**Responsibilities:**
- Monitor safety-critical alerts
- Ensure compliance with regulations
- Track air quality in tunnels
- Review safety inspection records

**Granted Capabilities:**
- Real-time alert monitoring
- View compliance-related data
- Acknowledge safety alerts
- Create safety inspection records

### IoT/Instrumentation Technician → ENGINEER
**Responsibilities:**
- Install and calibrate sensors
- Troubleshoot sensor issues
- Manage telemetry data quality
- Configure data collection parameters

**Granted Capabilities:**
- Full sensor management
- Configure sensor settings
- View diagnostic data
- Manage alert thresholds

### Executive/Project Sponsor → VIEWER
**Responsibilities:**
- Review high-level dashboards
- Monitor KPI performance
- Track project milestones
- Oversight and governance

**Granted Capabilities:**
- View all dashboards and reports
- Access KPI summaries
- Review asset portfolio status
- Read-only access for oversight

### Regulatory Inspector → VIEWER
**Responsibilities:**
- Audit compliance records
- Review inspection history
- Verify safety standards
- Access compliance documentation

**Granted Capabilities:**
- Read-only access to all data
- View inspection records
- Access compliance reports
- Review historical data

### Data Analyst/Asset Planner → ENGINEER
**Responsibilities:**
- Analyze historical trends
- Plan asset lifecycle
- Generate custom reports
- Forecast maintenance needs

**Granted Capabilities:**
- Full analytics access
- Export data for analysis
- Refresh KPI calculations
- Configure analytical parameters

---

## Implementation

### @RequiresPermission Annotation
Use this annotation on controller methods or service methods:

```java
@GetMapping("/sensors")
@RequiresPermission(Permission.SENSOR_READ)
public List<SensorDTO> getAllSensors() { ... }

@DeleteMapping("/sensors/{id}")
@RequiresPermission(Permission.SENSOR_DELETE)
public void deleteSensor(@PathVariable Long id) { ... }

// Multiple permissions (OR logic - any permission grants access)
@PutMapping("/alerts/{id}/resolve")
@RequiresPermission({Permission.ALERT_RESOLVE, Permission.SYSTEM_ADMIN})
public void resolveAlert(@PathVariable Long id) { ... }

// Multiple permissions (AND logic - all permissions required)
@PostMapping("/assets/bulk-import")
@RequiresPermission(value = {Permission.ASSET_WRITE, Permission.SENSOR_WRITE}, requireAll = true)
public void bulkImport(@RequestBody BulkImportRequest request) { ... }
```

### SecurityUtils
Programmatic permission checks:

```java
// Check current user's role
Optional<Role> role = SecurityUtils.getCurrentUserRole();

// Check specific permission
if (SecurityUtils.hasPermission(Permission.ALERT_RULE_MANAGE)) {
    // Allow rule management
}

// Check any of multiple permissions
if (SecurityUtils.hasAnyPermission(Permission.ALERT_ACKNOWLEDGE, Permission.ALERT_RESOLVE)) {
    // Allow alert action
}

// Require permission (throws AccessDeniedException if not granted)
SecurityUtils.requirePermission(Permission.SYSTEM_ADMIN);
```

### RolePermissionMapping
Get permissions for a role:

```java
// Get all permissions for a role
Set<Permission> engineerPermissions = RolePermissionMapping.getPermissions(Role.ENGINEER);

// Check if role has permission
boolean canExport = RolePermissionMapping.hasPermission(Role.OPERATOR, Permission.ANALYTICS_EXPORT);
// Returns: false

// Get roles that have a specific permission
Set<Role> rolesWithDelete = RolePermissionMapping.getRolesWithPermission(Permission.SENSOR_DELETE);
// Returns: [ADMIN]
```

---

## Security Best Practices

### Principle of Least Privilege
- Assign users the minimum role needed for their job function
- Use VIEWER role as default for new stakeholder accounts
- Escalate to higher roles only with documented justification

### Role Assignment Guidelines
1. **ADMIN**: Limited to 2-3 system administrators
2. **ENGINEER**: Technical staff with infrastructure responsibilities
3. **OPERATOR**: Field personnel and operations staff
4. **VIEWER**: Stakeholders requiring read-only oversight

### Audit Trail
- All permission checks are logged
- Role changes are tracked with timestamps and approver
- Failed authorization attempts trigger security alerts

### Session Management
- JWT tokens expire after 1 hour
- Refresh tokens valid for 7 days
- Force logout on role change
- Redis-based session tracking for token revocation
