package com.contoso.roadinfra.simulator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the simulator.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "simulation")
public class SimulatorConfig {

    /**
     * Interval between reading generations in seconds.
     */
    private int readingIntervalSeconds = 10;

    /**
     * Interval between sensor failure simulations in minutes.
     */
    private int failureIntervalMinutes = 60;

    /**
     * Time for sensor failure recovery in minutes.
     */
    private int failureRecoveryMinutes = 30;

    /**
     * Interval between anomaly injections in minutes.
     */
    private int anomalyIntervalMinutes = 120;

    /**
     * Battery drain percentage per reading cycle.
     */
    private double batteryDrainPerCycle = 0.01;

    /**
     * Whether simulation is enabled.
     */
    private boolean enabled = true;
}
