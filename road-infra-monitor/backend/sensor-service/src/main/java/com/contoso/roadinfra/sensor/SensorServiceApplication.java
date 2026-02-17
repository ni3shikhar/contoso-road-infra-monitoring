package com.contoso.roadinfra.sensor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.contoso.roadinfra.sensor", "com.contoso.roadinfra.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class SensorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorServiceApplication.class, args);
    }
}
