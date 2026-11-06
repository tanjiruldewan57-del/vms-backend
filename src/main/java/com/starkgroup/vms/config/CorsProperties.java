package com.starkgroup.vms.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Strongly typed CORS configuration properties.
 * Supports environment-based configuration via CORS_ALLOWED_ORIGINS and application.yml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vms.security.cors")
public class CorsProperties {

    /**
     * Whitelist of allowed origins. Defaults to local development endpoints and the deployed Netlify Flutter Web app.
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://127.0.0.1:3000",
            "http://localhost:3000",
            "http://127.0.0.1:8000",
            "http://localhost:8000",
            "https://bespoke-mochi-006b8f.netlify.app"
    ));

    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
    private boolean allowCredentials = true;
    private long maxAge = 3600L;

    /**
     * Sanitizes and parses allowed origins, supporting comma-separated inputs and stripping accidental trailing slashes.
     */
    public void setAllowedOrigins(List<String> origins) {
        if (origins != null) {
            List<String> sanitized = new ArrayList<>();
            for (String origin : origins) {
                if (origin != null && origin.contains(",")) {
                    for (String part : origin.split(",")) {
                        addNormalizedOrigin(sanitized, part);
                    }
                } else {
                    addNormalizedOrigin(sanitized, origin);
                }
            }
            if (!sanitized.isEmpty()) {
                this.allowedOrigins = sanitized;
            }
        }
    }

    /**
     * Post-construction hook guaranteeing CORS_ALLOWED_ORIGINS environment variable support.
     */
    @PostConstruct
    public void init() {
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (StringUtils.hasText(envOrigins)) {
            List<String> envList = new ArrayList<>();
            for (String part : envOrigins.split(",")) {
                addNormalizedOrigin(envList, part);
            }
            if (!envList.isEmpty()) {
                this.allowedOrigins = envList;
            }
        }
    }

    private void addNormalizedOrigin(List<String> list, String origin) {
        if (origin != null) {
            String trimmed = origin.trim();
            // Remove trailing slash if accidentally provided by deployment configs
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                list.add(trimmed);
            }
        }
    }
}
