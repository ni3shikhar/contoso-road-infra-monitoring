package com.contoso.roadinfra.common.constants;

/**
 * User roles for the Road Infrastructure Monitoring system.
 * 
 * Persona-to-Role Mapping:
 * - ADMIN: System administrators with full access
 * - ENGINEER: Structural/Civil Engineers, IoT/Instrumentation Techs, Data Analysts
 * - OPERATOR: Site/Project Managers, Maintenance/Ops Managers, Safety Officers
 * - VIEWER: Executives/Project Sponsors, Regulatory Inspectors
 */
public enum Role {
    ADMIN("Administrator"),
    ENGINEER("Engineer"),
    OPERATOR("Operator"),
    VIEWER("Viewer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
