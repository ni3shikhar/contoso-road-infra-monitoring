package com.contoso.roadinfra.alert.constants;

/**
 * Alert lifecycle status.
 */
public enum AlertStatus {
    /** Alert is open and requires attention */
    OPEN,
    
    /** Alert has been acknowledged by an operator */
    ACKNOWLEDGED,
    
    /** Alert is being actively worked on */
    IN_PROGRESS,
    
    /** Alert has been resolved */
    RESOLVED,
    
    /** Alert was dismissed as not requiring action */
    DISMISSED,
    
    /** Alert was escalated to higher level */
    ESCALATED,
    
    /** Alert was auto-resolved (condition cleared) */
    AUTO_RESOLVED;
    
    public boolean isActive() {
        return this == OPEN || this == ACKNOWLEDGED || this == IN_PROGRESS || this == ESCALATED;
    }
    
    public boolean isClosed() {
        return this == RESOLVED || this == DISMISSED || this == AUTO_RESOLVED;
    }
}
