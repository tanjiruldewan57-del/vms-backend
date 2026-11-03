package com.starkgroup.vms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Health check status response representing application and database connectivity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponse {

    private String status;
    private String database;
    private String databaseProductName;
    private String databaseProductVersion;
    private String error;

    @Builder.Default
    private Instant timestamp = Instant.now();
}

