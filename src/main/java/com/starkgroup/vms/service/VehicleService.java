package com.starkgroup.vms.service;

import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.VehicleStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining business operations for Vehicle lifecycle and management.
 */
public interface VehicleService {

    VehicleResponse createVehicle(VehicleCreateRequest request);

    VehicleResponse getVehicleById(Long id);

    VehicleResponse getVehicleByNumber(String vehicleNumber);

    PageResponse<VehicleResponse> getVehicles(String company, VehicleStatus status, String search, Pageable pageable);

    VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request);

    void deleteVehicle(Long id);
}
