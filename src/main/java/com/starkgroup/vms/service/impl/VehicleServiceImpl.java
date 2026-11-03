package com.starkgroup.vms.service.impl;

import com.starkgroup.vms.common.api.PageResponse;
import com.starkgroup.vms.common.exception.BusinessRuleViolationException;
import com.starkgroup.vms.common.exception.DuplicateResourceException;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleMileageUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleStatusUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.dto.response.VehicleStatsResponse;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.service.VehicleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Production implementation of {@link VehicleService} enforcing all domain business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleServiceImpl.class);
    private static final int MIN_FUEL_PERCENTAGE_FOR_DISPATCH = 10;

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        String sanitizedVin = sanitizeString(request.getVin()).toUpperCase(Locale.ROOT);
        String sanitizedPlate = sanitizeString(request.getLicensePlate()).toUpperCase(Locale.ROOT);

        validateUniqueIdentifiers(sanitizedVin, sanitizedPlate, null);
        validateManufactureYear(request.getYear());

        VehicleStatus initialStatus = request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE;

        Vehicle vehicle = Vehicle.builder()
                .vin(sanitizedVin)
                .licensePlate(sanitizedPlate)
                .make(sanitizeString(request.getMake()))
                .model(sanitizeString(request.getModel()))
                .year(request.getYear())
                .type(request.getType())
                .status(initialStatus)
                .fuelType(request.getFuelType())
                .mileage(request.getMileage())
                .fuelCapacity(request.getFuelCapacity())
                .currentFuelLevelPercentage(request.getCurrentFuelLevelPercentage())
                .color(sanitizeString(request.getColor()))
                .assignedDriverId(sanitizeString(request.getAssignedDriverId()))
                .lastServiceDate(request.getLastServiceDate())
                .nextServiceDueMileage(request.getNextServiceDueMileage())
                .notes(sanitizeString(request.getNotes()))
                .deleted(false)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Registered new vehicle in fleet: ID={}, VIN={}, Plate={}", saved.getId(), saved.getVin(), saved.getLicensePlate());
        return VehicleResponse.fromEntity(saved);
    }

    @Override
    public VehicleResponse getVehicleById(UUID id) {
        Vehicle vehicle = findActiveVehicleById(id);
        return VehicleResponse.fromEntity(vehicle);
    }

    @Override
    public VehicleResponse getVehicleByVin(String vin) {
        String sanitizedVin = sanitizeString(vin).toUpperCase(Locale.ROOT);
        Vehicle vehicle = vehicleRepository.findByVinIgnoreCaseAndDeletedFalse(sanitizedVin)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "vin", sanitizedVin));
        return VehicleResponse.fromEntity(vehicle);
    }

    @Override
    public PageResponse<VehicleResponse> getVehicles(VehicleStatus status, VehicleType type, String search, Pageable pageable) {
        Specification<Vehicle> spec = createVehicleSpecification(status, type, search);
        Page<Vehicle> page = vehicleRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(VehicleResponse::fromEntity));
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID id, VehicleUpdateRequest request) {
        Vehicle vehicle = findActiveVehicleById(id);

        if (vehicle.getStatus() == VehicleStatus.DECOMMISSIONED) {
            throw new BusinessRuleViolationException("Cannot update specifications of a decommissioned vehicle.");
        }

        validateManufactureYear(request.getYear());

        vehicle.setMake(sanitizeString(request.getMake()));
        vehicle.setModel(sanitizeString(request.getModel()));
        vehicle.setYear(request.getYear());
        vehicle.setType(request.getType());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setFuelCapacity(request.getFuelCapacity());
        vehicle.setCurrentFuelLevelPercentage(request.getCurrentFuelLevelPercentage());
        vehicle.setColor(sanitizeString(request.getColor()));
        vehicle.setAssignedDriverId(sanitizeString(request.getAssignedDriverId()));
        vehicle.setLastServiceDate(request.getLastServiceDate());
        vehicle.setNextServiceDueMileage(request.getNextServiceDueMileage());
        vehicle.setNotes(sanitizeString(request.getNotes()));

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Updated specifications for vehicle ID={}", id);
        return VehicleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(UUID id, VehicleStatusUpdateRequest request) {
        Vehicle vehicle = findActiveVehicleById(id);
        VehicleStatus currentStatus = vehicle.getStatus();
        VehicleStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            return VehicleResponse.fromEntity(vehicle);
        }

        // Rule 1: Decommissioned vehicles are permanently retired
        if (currentStatus == VehicleStatus.DECOMMISSIONED) {
            throw new BusinessRuleViolationException("Decommissioned vehicles cannot be transitioned back to active service.");
        }

        // Rule 2: Cannot transition directly from UNDER_MAINTENANCE to IN_USE without passing through AVAILABLE
        if (currentStatus == VehicleStatus.UNDER_MAINTENANCE && targetStatus == VehicleStatus.IN_USE) {
            throw new BusinessRuleViolationException(
                    "Vehicles currently UNDER_MAINTENANCE must be inspected and marked AVAILABLE before being placed IN_USE."
            );
        }

        // Rule 3: Minimum fuel/battery level check before dispatching IN_USE
        if (targetStatus == VehicleStatus.IN_USE) {
            Integer fuelLevel = vehicle.getCurrentFuelLevelPercentage();
            if (fuelLevel != null && fuelLevel < MIN_FUEL_PERCENTAGE_FOR_DISPATCH) {
                throw new BusinessRuleViolationException(
                        String.format("Cannot deploy vehicle into use with low fuel/battery level (%d%%). Minimum required is %d%%.",
                                fuelLevel, MIN_FUEL_PERCENTAGE_FOR_DISPATCH)
                );
            }
        }

        vehicle.setStatus(targetStatus);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            String noteEntry = String.format("[Status -> %s]: %s", targetStatus, request.getReason().trim());
            String existingNotes = vehicle.getNotes() != null ? vehicle.getNotes() + "\n" : "";
            vehicle.setNotes(existingNotes + noteEntry);
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle ID={} transitioned status from {} to {}", id, currentStatus, targetStatus);
        return VehicleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleMileage(UUID id, VehicleMileageUpdateRequest request) {
        Vehicle vehicle = findActiveVehicleById(id);
        double currentMileage = vehicle.getMileage();
        double newMileage = request.getMileage();

        // Business Rule: Odometer reading must be monotonic non-decreasing
        if (newMileage < currentMileage) {
            throw new BusinessRuleViolationException(
                    String.format("New odometer reading (%.2f km) cannot be less than existing recorded mileage (%.2f km).",
                            newMileage, currentMileage)
            );
        }

        vehicle.setMileage(newMileage);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String noteEntry = String.format("[Mileage Log %.2f km]: %s", newMileage, request.getNotes().trim());
            String existingNotes = vehicle.getNotes() != null ? vehicle.getNotes() + "\n" : "";
            vehicle.setNotes(existingNotes + noteEntry);
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle ID={} mileage updated from {} to {}", id, currentMileage, newMileage);
        return VehicleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteVehicle(UUID id) {
        Vehicle vehicle = findActiveVehicleById(id);
        vehicle.setDeleted(true);
        vehicle.setStatus(VehicleStatus.DECOMMISSIONED);
        vehicleRepository.save(vehicle);
        log.info("Soft deleted vehicle ID={} and marked status as DECOMMISSIONED", id);
    }

    @Override
    public VehicleStatsResponse getVehicleStatistics() {
        long total = vehicleRepository.countByDeletedFalse();
        long available = vehicleRepository.countByStatusAndDeletedFalse(VehicleStatus.AVAILABLE);
        long inUse = vehicleRepository.countByStatusAndDeletedFalse(VehicleStatus.IN_USE);
        long maintenance = vehicleRepository.countByStatusAndDeletedFalse(VehicleStatus.UNDER_MAINTENANCE);
        long decommissioned = vehicleRepository.countByStatusAndDeletedFalse(VehicleStatus.DECOMMISSIONED);
        Double avgMileage = vehicleRepository.getAverageMileage();

        return VehicleStatsResponse.builder()
                .totalVehicles(total)
                .availableCount(available)
                .inUseCount(inUse)
                .underMaintenanceCount(maintenance)
                .decommissionedCount(decommissioned)
                .averageMileage(avgMileage != null ? avgMileage : 0.0)
                .build();
    }

    private Vehicle findActiveVehicleById(UUID id) {
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
    }

    private void validateUniqueIdentifiers(String vin, String licensePlate, UUID excludeId) {
        boolean vinExists = excludeId == null
                ? vehicleRepository.existsByVinIgnoreCaseAndDeletedFalse(vin)
                : vehicleRepository.existsByVinIgnoreCaseAndIdNotAndDeletedFalse(vin, excludeId);

        if (vinExists) {
            throw new DuplicateResourceException("Vehicle", "vin", vin);
        }

        boolean plateExists = excludeId == null
                ? vehicleRepository.existsByLicensePlateIgnoreCaseAndDeletedFalse(licensePlate)
                : vehicleRepository.existsByLicensePlateIgnoreCaseAndIdNotAndDeletedFalse(licensePlate, excludeId);

        if (plateExists) {
            throw new DuplicateResourceException("Vehicle", "licensePlate", licensePlate);
        }
    }

    private void validateManufactureYear(int year) {
        int maxAllowedYear = Year.now().getValue() + 1;
        if (year < 1900 || year > maxAllowedYear) {
            throw new BusinessRuleViolationException(
                    String.format("Manufacture year %d is out of valid range (1900 - %d)", year, maxAllowedYear)
            );
        }
    }

    private String sanitizeString(String value) {
        return value == null ? null : value.trim();
    }

    private Specification<Vehicle> createVehicleSpecification(VehicleStatus status, VehicleType type, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate makeMatch = cb.like(cb.lower(root.get("make")), searchPattern);
                Predicate modelMatch = cb.like(cb.lower(root.get("model")), searchPattern);
                Predicate vinMatch = cb.like(cb.lower(root.get("vin")), searchPattern);
                Predicate plateMatch = cb.like(cb.lower(root.get("licensePlate")), searchPattern);
                predicates.add(cb.or(makeMatch, modelMatch, vinMatch, plateMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

