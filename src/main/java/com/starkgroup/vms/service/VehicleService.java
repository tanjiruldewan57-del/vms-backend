package com.starkgroup.vms.service;

import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleMileageUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleStatusUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.dto.response.VehicleStatsResponse;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract defining vehicle business operations and fleet management rules.
 */
public interface VehicleService {

    VehicleResponse createVehicle(VehicleCreateRequest request);

    VehicleResponse getVehicleById(UUID id);

    VehicleResponse getVehicleByVin(String vin);

    PageResponse<VehicleResponse> getVehicles(VehicleStatus status, VehicleType type, String search, Pageable pageable);

    VehicleResponse updateVehicle(UUID id, VehicleUpdateRequest request);

    VehicleResponse updateVehicleStatus(UUID id, VehicleStatusUpdateRequest request);

    VehicleResponse updateVehicleMileage(UUID id, VehicleMileageUpdateRequest request);

    void deleteVehicle(UUID id);

    VehicleStatsResponse getVehicleStatistics();
}

