// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleAccessTest {

    private static final String OWNER_ID = "owner-123";

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserUtils userUtils;

    private VehicleAccess vehicleAccess;
    private final ObjectId vehicleId = new ObjectId();

    @BeforeEach
    void setUp() {
        vehicleAccess = new VehicleAccess(vehicleRepository, userUtils);
    }

    private Vehicle vehicleOf(String ownerId) {
        return new Vehicle(
                ownerId,
                "Gol Azul",
                "Volkswagen Gol",
                "ABC1234",
                "14283256656",
                "Azul",
                VehicleType.HATCH,
                5,
                List.of(new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89")))
        );
    }

    @Test
    @DisplayName("Deve devolver o veículo ativo do dono informado, sem depender de quem está logado")
    void shouldReturnActiveVehicleOfTheGivenOwner() {
        Vehicle vehicle = vehicleOf(OWNER_ID);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        Vehicle found = vehicleAccess.getActiveVehicleOf(vehicleId.toHexString(), OWNER_ID);

        assertThat(found).isSameAs(vehicle);
        verify(userUtils, never()).getCurrentUser(any(String.class));
    }

    @Test
    @DisplayName("Veículo de outro dono é tratado como não encontrado")
    void shouldNotReturnVehicleOfAnotherOwner() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicleOf("outro-dono")));

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> vehicleAccess.getActiveVehicleOf(vehicleId.toHexString(), OWNER_ID));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("Veículo desativado é tratado como não encontrado")
    void shouldNotReturnInactiveVehicle() {
        Vehicle vehicle = vehicleOf(OWNER_ID);
        vehicle.deactivate();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> vehicleAccess.getActiveVehicleOf(vehicleId.toHexString(), OWNER_ID));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("Id nulo, inválido ou inexistente é veículo não encontrado")
    void shouldFailWhenIdIsNullInvalidOrUnknown() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThrows(ApplicationException.class, () -> vehicleAccess.getActiveVehicleOf(null, OWNER_ID));
        assertThrows(ApplicationException.class, () -> vehicleAccess.getActiveVehicleOf("id-invalido", OWNER_ID));
        assertThrows(ApplicationException.class,
                () -> vehicleAccess.getActiveVehicleOf(vehicleId.toHexString(), OWNER_ID));
    }
}
