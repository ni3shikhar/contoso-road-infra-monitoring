package com.contoso.roadinfra.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Standalone data simulator for the Road Infrastructure Monitoring System.
 * Generates realistic sensor data for all sensors deployed across the 2km corridor.
 */
@SpringBootApplication
@EnableScheduling
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
