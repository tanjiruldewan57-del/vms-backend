package com.starkgroup.vms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Strongly typed CORS configuration properties.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vms.security.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://127.0.0.1:3000", "http://localhost:3000"));
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}

