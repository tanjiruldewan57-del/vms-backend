package com.starkgroup.vms.controller;

import com.starkgroup.vms.common.api.ApiResponse;
import com.starkgroup.vms.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;

/**
 * Lightweight health check endpoint verifying database connectivity.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            DatabaseMetaData metaData = connection.getMetaData();

            HealthResponse health = HealthResponse.builder()
                    .status(valid ? "UP" : "DEGRADED")
                    .database(valid ? "CONNECTED" : "UNREACHABLE")
                    .databaseProductName(metaData.getDatabaseProductName())
                    .databaseProductVersion(metaData.getDatabaseProductVersion())
                    .timestamp(Instant.now())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(health, "System and database are healthy"));
        } catch (Exception ex) {
            log.error("Database health check probe failed", ex);
            HealthResponse health = HealthResponse.builder()
                    .status("DOWN")
                    .database("DISCONNECTED")
                    .error(ex.getMessage())
                    .timestamp(Instant.now())
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<HealthResponse>builder()
                            .success(false)
                            .message("Database connectivity failure")
                            .data(health)
                            .timestamp(Instant.now())
                            .build());
        }
    }
}

