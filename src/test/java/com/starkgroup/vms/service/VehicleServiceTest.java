package com.starkgroup.vms.service;

import com.starkgroup.vms.common.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.dto.request.VehicleCreateRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for Vehicle business rules, specifically the duplicate Vehicle Number invariant.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    @DisplayName("createVehicle: When vehicleNumber is unique, should save vehicle successfully")
    void createVehicle_WhenVehicleNumberIsUnique_ShouldSaveAndReturnResponse() {
        // Arrange
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

        // Act
        VehicleResponse response = vehicleService.createVehicle(request);

        // Assert
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
        // Arrange
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("VMS-101")
                .company("Wayne Enterprises")
                .type("Truck")
                .brand("Volvo")
                .model("FH16")
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-101")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-101' already exists in the system.")
                .satisfies(ex -> {
                    DuplicateVehicleNumberException dupEx = (DuplicateVehicleNumberException) ex;
                    assertThat(dupEx.getVehicleNumber()).isEqualTo("VMS-101");
                });

        // Ensure persistence layer was never invoked
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("createVehicle: When vehicleNumber has mixed-case or surrounding whitespace, should sanitize and detect duplicate")
    void createVehicle_WhenVehicleNumberDiffersByCaseOrWhitespace_ShouldSanitizeAndDetectDuplicate() {
        // Arrange: Input is lowercase with whitespace
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .vehicleNumber("  vms-101  ")
                .company("Acme Corp")
                .type("Sedan")
                .brand("Toyota")
                .model("Camry")
                .build();

        // Sanitized version "VMS-101" already exists
        when(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-101")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessage("Vehicle with number 'VMS-101' already exists in the system.");

        verify(vehicleRepository).existsByVehicleNumberIgnoreCase("VMS-101");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
