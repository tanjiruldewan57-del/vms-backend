package com.starkgroup.vms.service;

import com.starkgroup.vms.common.exception.BusinessRuleViolationException;
import com.starkgroup.vms.common.exception.DuplicateResourceException;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleMileageUpdateRequest;
import com.starkgroup.vms.dto.request.VehicleStatusUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Vehicle sampleVehicle;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        sampleVehicle = Vehicle.builder()
                .id(vehicleId)
                .vin("1HGCR2F83HA123456")
                .licensePlate("ABC-1234")
                .make("Toyota")
                .model("RAV4")
                .year(2023)
                .type(VehicleType.SUV)
                .status(VehicleStatus.AVAILABLE)
                .fuelType(FuelType.HYBRID)
                .mileage(12000.0)
                .fuelCapacity(55.0)
                .currentFuelLevelPercentage(70)
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should create vehicle when identifiers are unique")
    void testCreateVehicle_Success() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vin("1HGCR2F83HA123456")
                .licensePlate("ABC-1234")
                .make("Toyota")
                .model("RAV4")
                .year(2023)
                .type(VehicleType.SUV)
                .fuelType(FuelType.HYBRID)
                .mileage(12000.0)
                .build();

        when(vehicleRepository.existsByVinIgnoreCaseAndDeletedFalse(request.getVin())).thenReturn(false);
        when(vehicleRepository.existsByLicensePlateIgnoreCaseAndDeletedFalse(request.getLicensePlate())).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        VehicleResponse response = vehicleService.createVehicle(request);

        assertThat(response).isNotNull();
        assertThat(response.getVin()).isEqualTo(request.getVin());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Should reject vehicle creation when VIN already exists")
    void testCreateVehicle_DuplicateVin_ThrowsException() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vin("1HGCR2F83HA123456")
                .licensePlate("ABC-1234")
                .make("Toyota")
                .model("RAV4")
                .year(2023)
                .type(VehicleType.SUV)
                .fuelType(FuelType.HYBRID)
                .mileage(12000.0)
                .build();

        when(vehicleRepository.existsByVinIgnoreCaseAndDeletedFalse(request.getVin())).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Vehicle already exists with vin");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject odometer rollback (monotonic mileage business rule)")
    void testUpdateMileage_DecreasingOdometer_ThrowsBusinessRuleViolation() {
        VehicleMileageUpdateRequest request = VehicleMileageUpdateRequest.builder()
                .mileage(11000.0) // current is 12000.0
                .build();

        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> vehicleService.updateVehicleMileage(vehicleId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be less than existing recorded mileage");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow valid forward odometer mileage increase")
    void testUpdateMileage_MonotonicIncrease_Success() {
        VehicleMileageUpdateRequest request = VehicleMileageUpdateRequest.builder()
                .mileage(13500.0)
                .notes("Highway trip inspection")
                .build();

        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicleMileage(vehicleId, request);

        assertThat(response.getMileage()).isEqualTo(13500.0);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Should prevent reviving decommissioned vehicle to active status")
    void testUpdateStatus_DecommissionedVehicle_ThrowsBusinessRuleViolation() {
        sampleVehicle.setStatus(VehicleStatus.DECOMMISSIONED);

        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> vehicleService.updateVehicleStatus(vehicleId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Decommissioned vehicles cannot be transitioned back");
    }

    @Test
    @DisplayName("Should prevent dispatching vehicle directly from UNDER_MAINTENANCE to IN_USE")
    void testUpdateStatus_MaintenanceToInUse_ThrowsBusinessRuleViolation() {
        sampleVehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);

        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.IN_USE)
                .build();

        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> vehicleService.updateVehicleStatus(vehicleId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("must be inspected and marked AVAILABLE before being placed IN_USE");
    }

    @Test
    @DisplayName("Should prevent dispatching vehicle with fuel/battery level < 10%")
    void testUpdateStatus_LowFuelToInUse_ThrowsBusinessRuleViolation() {
        sampleVehicle.setStatus(VehicleStatus.AVAILABLE);
        sampleVehicle.setCurrentFuelLevelPercentage(5);

        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.IN_USE)
                .build();

        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));

        assertThatThrownBy(() -> vehicleService.updateVehicleStatus(vehicleId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot deploy vehicle into use with low fuel/battery level");
    }

    @Test
    @DisplayName("Should soft-delete vehicle and mark status as DECOMMISSIONED")
    void testDeleteVehicle_SetsDeletedTrue() {
        when(vehicleRepository.findByIdAndDeletedFalse(vehicleId)).thenReturn(Optional.of(sampleVehicle));

        vehicleService.deleteVehicle(vehicleId);

        assertThat(sampleVehicle.isDeleted()).isTrue();
        assertThat(sampleVehicle.getStatus()).isEqualTo(VehicleStatus.DECOMMISSIONED);
        verify(vehicleRepository).save(sampleVehicle);
    }
}

