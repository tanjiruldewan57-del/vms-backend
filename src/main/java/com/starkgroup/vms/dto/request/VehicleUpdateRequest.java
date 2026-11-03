package com.starkgroup.vms.dto.request;

import com.starkgroup.vms.model.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for updating vehicle specifications and operational details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequest {

    @NotBlank(message = "Vehicle number is required")
    @Size(min = 2, max = 50, message = "Vehicle number must be between 2 and 50 characters")
    private String vehicleNumber;

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company cannot exceed 100 characters")
    private String company;

    @NotBlank(message = "Vehicle type is required")
    @Size(max = 50, message = "Vehicle type cannot exceed 50 characters")
    private String type;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    @NotNull(message = "Vehicle status is required")
    private VehicleStatus status;
}
