package com.starkgroup.vms.service;

import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.common.exception.ResourceNotFoundException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
import com.starkgroup.vms.dto.request.VehicleUpdateRequest;
import com.starkgroup.vms.dto.response.VehicleResponse;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.repository.VehicleRepository;
import com.starkgroup.vms.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying domain business rules in the service layer,
 * specifically the duplicate Vehicle Number invariant during create and update.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    // =========================================================================
    // 1. CREATE VEHICLE TESTS
    // =========================================================================

    @Test
    @DisplayName("createVehicle: When vehicleNumber is unique, should save vehicle successfully")
    void createVehicle_WhenVehicleNumberIsUnique_ShouldSaveAndReturnResponse() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        Vehicle persistedVehicle = Vehicle.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Logistics")
                .type("Van")
                .brand("Ford")
                .model("Transit")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-101")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(persistedVehicle);

        VehicleResponse response = vehicleService.createVehicle(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getVehicleNumber()).isEqualTo("VMS-101");
        assertThat(response.getCompany()).isEqualTo("Stark Logistics");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        assertThat(vehicleCaptor.getValue().getVehicleNumber()).isEqualTo("VMS-101");
    }

    @Test
    @DisplayName("createVehicle: When vehicleNumber already exists, should throw DuplicateVehicleNumberException and prevent saving")
    void createVehicle_WhenVehicleNumberAlreadyExists_ShouldThrowDuplicateVehicleNumberException() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Wayne Enterprises")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-101")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-101' already exists in the system.")
                .satisfies(ex -> {
                    DuplicateVehicleNumberException dupEx = (DuplicateVehicleNumberException) ex;
                    assertThat(dupEx.getVehicleNumber()).isEqualTo("VMS-101");
                });

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("createVehicle: When vehicleNumber has mixed-case or surrounding whitespace, should sanitize and detect duplicate")
    void createVehicle_WhenVehicleNumberDiffersByCaseOrWhitespace_ShouldSanitizeAndDetectDuplicate() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("  vms-101  ")
                .company("Acme Corp")
                .type("Sedan")
                .brand("Toyota")
                .model("Camry")
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-101")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-101' already exists in the system.");

        verify(vehicleRepository).existsByVehicleNumberIgnoreCase("VMS-101");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // =========================================================================
    // 2. UPDATE VEHICLE TESTS
    // =========================================================================

    @Test
    @DisplayName("updateVehicle: When updated vehicleNumber conflicts with another vehicle, should throw DuplicateVehicleNumberException")
    void updateVehicle_WhenUpdatedNumberConflictsWithOtherVehicle_ShouldThrowDuplicateException() {
        Vehicle existingVehicle = Vehicle.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Group")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build();

        VehicleUpdateRequest updateRequest = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-202") // Conflicts with vehicle ID 2
                .company("Stark Group")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndIdNot("VMS-202", 1L)).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.updateVehicle(1L, updateRequest))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-202' already exists in the system.");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("updateVehicle: When updated vehicleNumber is same as current vehicle, should succeed")
    void updateVehicle_WhenSameNumber_ShouldSucceed() {
        Vehicle existingVehicle = Vehicle.builder()
                .id(1L)
                .vehicleNumber("VMS-101")
                .company("Stark Group")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build();

        VehicleUpdateRequest updateRequest = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Enterprise")
                .type("Truck")
                .brand("Scania")
                .model("R500 NextGen")
                .status(VehicleStatus.IN_USE)
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.existsByVehicleNumberIgnoreCaseAndIdNot("VMS-101", 1L)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getCompany()).isEqualTo("Stark Enterprise");
        assertThat(response.getModel()).isEqualTo("R500 NextGen");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        verify(vehicleRepository).save(existingVehicle);
    }

    @Test
    @DisplayName("updateVehicle: When ID does not exist, should throw ResourceNotFoundException")
    void updateVehicle_WhenNotFound_ShouldThrowResourceNotFoundException() {
        VehicleUpdateRequest updateRequest = VehicleUpdateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Stark Group")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.updateVehicle(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vehicle not found with id: '999'");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
