package com.contoso.roadinfra.common.constants;

/**
 * Granular permissions for the Road Infrastructure Monitoring system.
 */
public enum Permission {
    // Sensor permissions
    SENSOR_READ("Read sensor data"),
    SENSOR_WRITE("Create/update sensors"),
    SENSOR_DELETE("Delete sensors"),
    SENSOR_CONFIGURE("Configure sensor settings"),

    // Asset permissions
    ASSET_READ("Read asset data"),
    ASSET_WRITE("Create/update assets"),
    ASSET_DELETE("Delete assets"),
    ASSET_PROGRESS_UPDATE("Update asset progress"),

    // Monitoring permissions
    MONITORING_READ("Read monitoring data"),
    MONITORING_CONFIGURE_THRESHOLDS("Configure monitoring thresholds"),

    // Alert permissions
    ALERT_READ("Read alerts"),
    ALERT_ACKNOWLEDGE("Acknowledge alerts"),
    ALERT_ASSIGN("Assign alerts"),
    ALERT_RESOLVE("Resolve alerts"),
    ALERT_RULE_MANAGE("Manage alert rules"),

    // Analytics permissions
    ANALYTICS_READ("Read analytics data"),
    ANALYTICS_EXPORT("Export analytics data"),
    ANALYTICS_REFRESH("Refresh analytics data"),

    // Inspection permissions
    INSPECTION_READ("Read inspection data"),
    INSPECTION_WRITE("Create/update inspections"),

    // User management permissions
    USER_READ("Read user data"),
    USER_MANAGE("Manage users"),

    // System administration
    SYSTEM_ADMIN("Full system administration");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
