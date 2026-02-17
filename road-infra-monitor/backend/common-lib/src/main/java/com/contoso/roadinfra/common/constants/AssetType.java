package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetType {
    ROAD("Road"),
    ROAD_SECTION("Road Section"),
    BRIDGE("Bridge"),
    TUNNEL("Tunnel"),
    DRAINAGE("Drainage"),
    GUARDRAIL("Guardrail"),
    LIGHTING("Lighting"),
    INTERCHANGE("Interchange"),
    INTERSECTION("Intersection"),
    RETAINING_WALL("Retaining Wall"),
    SIGN("Sign"),
    CULVERT("Culvert");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AssetType fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (AssetType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName) || type.name().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown asset type: " + displayName);
    }
}
