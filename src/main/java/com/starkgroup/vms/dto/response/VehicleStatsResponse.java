package com.starkgroup.vms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metric summary of fleet distribution and operational health.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatsResponse {

    private long totalVehicles;
    private long availableCount;
    private long inUseCount;
    private long underMaintenanceCount;
    private long decommissionedCount;
    private double averageMileage;
}

