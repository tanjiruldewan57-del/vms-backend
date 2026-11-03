package com.starkgroup.vms.dto.request;

import com.starkgroup.vms.model.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for transitioning a vehicle's operational lifecycle status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusUpdateRequest {

    @NotNull(message = "Target status is required")
    private VehicleStatus status;

    @Size(max = 255, message = "Status change reason cannot exceed 255 characters")
    private String reason;
}

