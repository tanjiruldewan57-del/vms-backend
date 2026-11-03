package com.starkgroup.vms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for logging new odometer mileage readings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMileageUpdateRequest {

    @NotNull(message = "New odometer mileage is required")
    @PositiveOrZero(message = "Mileage must be positive or zero")
    private Double mileage;

    @Size(max = 255, message = "Trip or odometer update notes cannot exceed 255 characters")
    private String notes;
}

