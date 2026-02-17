package com.contoso.roadinfra.alert.constants;

/**
 * Source type of the alert.
 */
public enum SourceType {
    /** Alert triggered by sensor reading threshold violation */
    SENSOR,
    
    /** Alert triggered by asset health status change */
    HEALTH,
    
    /** Alert triggered by system event */
    SYSTEM,
    
    /** Alert created manually by user */
    MANUAL,
    
    /** Alert triggered by scheduled inspection */
    INSPECTION,
    
    /** Alert triggered by external integration */
    EXTERNAL,
    
    /** Alert triggered by maintenance schedule */
    MAINTENANCE,
    
    /** Alert triggered by security event */
    SECURITY
}
