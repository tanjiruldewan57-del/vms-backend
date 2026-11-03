package com.starkgroup.vms.controller;

import com.starkgroup.vms.common.api.ApiResponse;
import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleMileageUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleStatusUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.dto.response.VehicleStatsResponse;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import com.starkgroup.vms.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller exposing Vehicle Management endpoints under /api/v1/vehicles.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Vehicle successfully registered in fleet"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> getVehicles(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<VehicleResponse> vehicles = vehicleService.getVehicles(status, type, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Vehicles retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable UUID id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/vin/{vin}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleByVin(@PathVariable String vin) {
        VehicleResponse response = vehicleService.getVehicleByVin(vin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<VehicleStatsResponse>> getVehicleStats() {
        VehicleStatsResponse stats = vehicleService.getVehicleStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Fleet statistics calculated successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleUpdateRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle specifications updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleStatusUpdateRequest request) {
        VehicleResponse response = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle status updated successfully"));
    }

    @PatchMapping("/{id}/mileage")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleMileage(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleMileageUpdateRequest request) {
        VehicleResponse response = vehicleService.updateVehicleMileage(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Odometer mileage recorded successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}

