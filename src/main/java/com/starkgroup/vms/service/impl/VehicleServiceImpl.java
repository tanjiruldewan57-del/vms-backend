package com.starkgroup.vms.service.impl;

import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Service implementation managing vehicle domain logic.
 * Encapsulates all business rules such as vehicle number uniqueness enforcement.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleServiceImpl.class);

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        String sanitizedVehicleNumber = sanitizeVehicleNumber(request.getVehicleNumber());

        // Business Rule: Vehicle Number must be globally unique
        if (vehicleRepository.existsByVehicleNumberIgnoreCase(sanitizedVehicleNumber)) {
            log.warn("Attempt to register existing vehicle number: {}", sanitizedVehicleNumber);
            throw new DuplicateVehicleNumberException(sanitizedVehicleNumber);
        }

        VehicleStatus initialStatus = request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE;

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(sanitizedVehicleNumber)
                .company(request.getCompany().trim())
                .type(request.getType().trim())
                .brand(request.getBrand().trim())
                .model(request.getModel().trim())
                .status(initialStatus)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle successfully registered with ID={} and VehicleNumber={}", saved.getId(), saved.getVehicleNumber());
        return VehicleResponse.fromEntity(saved);
    }

    @Override
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        return VehicleResponse.fromEntity(vehicle);
    }

    @Override
    public VehicleResponse getVehicleByNumber(String vehicleNumber) {
        String sanitizedVehicleNumber = sanitizeVehicleNumber(vehicleNumber);
        Vehicle vehicle = vehicleRepository.findByVehicleNumberIgnoreCase(sanitizedVehicleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "vehicleNumber", sanitizedVehicleNumber));
        return VehicleResponse.fromEntity(vehicle);
    }

    @Override
    public PageResponse<VehicleResponse> getAllVehicles(Pageable pageable) {
        Page<Vehicle> page = vehicleRepository.findAll(pageable);
        return PageResponse.from(page.map(VehicleResponse::fromEntity));
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));

        vehicle.setCompany(request.getCompany().trim());
        vehicle.setType(request.getType().trim());
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setStatus(request.getStatus());

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle ID={} updated successfully", id);
        return VehicleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));

        vehicleRepository.delete(vehicle);
        log.info("Vehicle ID={} deleted from system", id);
    }

    private String sanitizeVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            return "";
        }
        return vehicleNumber.trim().toUpperCase(Locale.ROOT);
    }
}
