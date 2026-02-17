package com.contoso.roadinfra.simulator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API connection configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    private Gateway gateway = new Gateway();
    private Auth auth = new Auth();

    @Data
    public static class Gateway {
        private String url = "http://localhost:8080";
    }

    @Data
    public static class Auth {
        private String username = "admin";
        private String password = "Admin@123";
    }
}
