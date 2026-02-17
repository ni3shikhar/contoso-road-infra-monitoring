package com.contoso.roadinfra.common.exception;

public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;
    private final String reason;

    public ServiceUnavailableException(String serviceName, String reason) {
        super(String.format("Service '%s' is unavailable: %s", serviceName, reason));
        this.serviceName = serviceName;
        this.reason = reason;
    }

    public ServiceUnavailableException(String serviceName) {
        super(String.format("Service '%s' is currently unavailable", serviceName));
        this.serviceName = serviceName;
        this.reason = "Unknown";
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getReason() {
        return reason;
    }
}
