package com.starkgroup.vms.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 / RFC 9110 compliant error response structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private List<ValidationErrorDetail> fieldErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorDetail {
        private String field;
        private Object rejectedValue;
        private String message;
    }
}

