package com.starkgroup.vms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly typed configuration properties for JWT token creation and validation.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vms.security.jwt")
public class JwtProperties {

    /**
     * Secret key for HMAC-SHA256 signing (minimum 256 bits).
     */
    private String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B59703273357638792F423F4528482B4D6251655468576D5A7134743777217A25432A";

    /**
     * Access token validity in minutes.
     */
    private long expirationMinutes = 60;

    /**
     * Refresh token validity in days.
     */
    private long refreshExpirationDays = 7;
}

