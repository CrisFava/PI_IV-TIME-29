package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.CreateVehicleUseCase;
import com.converge.api.modules.user.application.DeleteVehicleUseCase;
import com.converge.api.modules.user.application.GetVehicleUseCase;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.application.dto.CreateVehicleCommand;
import com.converge.api.modules.user.application.dto.CreateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.FuelSourceRequestDTO;
import com.converge.api.modules.user.application.dto.VehicleRefCommand;
import com.converge.api.modules.user.application.mapper.VehicleMapper;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
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
class VehicleUseCasesTest {

    private static final String UID = "firebase-uid-123";

    @Mock
    private UserUtils userUtils;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private User user;

    private VehicleMapper mapper;
    private VehicleAccess vehicleAccess;
    private final ObjectId userObjectId = new ObjectId();

    @BeforeEach
    void setUp() {
        mapper = new VehicleMapper();
        vehicleAccess = new VehicleAccess(vehicleRepository, userUtils);
    }

    private CreateVehicleRequestDTO validRequest() {
        return new CreateVehicleRequestDTO(
                "Gol Azul",
                "Volkswagen Gol",
                "abc-1234",
                "Azul",
                VehicleType.HATCH,
                5,
                List.of(new FuelSourceRequestDTO(EnergySource.GASOLINE, 12.0, new BigDecimal("5.89")))
        );
    }

    private Vehicle ownedVehicle(String ownerId) {
        return new Vehicle(
                ownerId,
                "Gol Azul",
                "Volkswagen Gol",
                "ABC1234",
                "Azul",
                VehicleType.HATCH,
                5,
                List.of(new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89")))
        );
    }

    @Test
    @DisplayName("Quem não é motorista não pode cadastrar veículo")
    void shouldRejectCreationWhenUserIsNotDriver() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(false);

        assertThrows(ApplicationException.class,
                () -> createVehicle.execute(new CreateVehicleCommand(UID, validRequest())));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motorista deve cadastrar veículo com a placa normalizada")
    void shouldCreateVehicleForDriver() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findByUserIdAndLicensePlate(userObjectId.toHexString(), "ABC1234")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle created = createVehicle.execute(new CreateVehicleCommand(UID, validRequest()));

        assertThat(created.getLicensePlate()).isEqualTo("ABC1234");
        assertThat(created.getUserId()).isEqualTo(userObjectId.toHexString());
    }

    @Test
    @DisplayName("Não deve cadastrar duas vezes a mesma placa ativa para o mesmo dono")
    void shouldRejectDuplicatedPlate() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findByUserIdAndLicensePlate(userObjectId.toHexString(), "ABC1234"))
                .thenReturn(Optional.of(ownedVehicle(userObjectId.toHexString())));

        assertThrows(ApplicationException.class,
                () -> createVehicle.execute(new CreateVehicleCommand(UID, validRequest())));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Placa de veículo excluído deve reativar o veículo antigo com os dados novos")
    void shouldReactivateDeletedVehicleWithSamePlate() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper);
        Vehicle deleted = ownedVehicle(userObjectId.toHexString());
        deleted.deactivate();
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findByUserIdAndLicensePlate(userObjectId.toHexString(), "ABC1234"))
                .thenReturn(Optional.of(deleted));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle result = createVehicle.execute(new CreateVehicleCommand(UID, validRequest()));

        assertThat(result).isSameAs(deleted);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getColor()).isEqualTo("Azul");
        assertThat(result.getModel()).isEqualTo("Volkswagen Gol");
    }

    @Test
    @DisplayName("Não deve devolver o veículo de outro usuário")
    void shouldNotFindVehicleOfAnotherUser() {
        GetVehicleUseCase getVehicle = new GetVehicleUseCase(vehicleAccess);
        ObjectId vehicleId = new ObjectId();
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(ownedVehicle("outro-dono")));

        assertThrows(ApplicationException.class,
                () -> getVehicle.execute(new VehicleRefCommand(vehicleId.toHexString(), UID)));
    }

    @Test
    @DisplayName("Excluir veículo deve apenas desativá-lo")
    void shouldDeactivateVehicleOnDelete() {
        DeleteVehicleUseCase deleteVehicle = new DeleteVehicleUseCase(vehicleAccess, vehicleRepository);
        ObjectId vehicleId = new ObjectId();
        Vehicle vehicle = ownedVehicle(userObjectId.toHexString());
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        deleteVehicle.execute(new VehicleRefCommand(vehicleId.toHexString(), UID));

        assertThat(vehicle.isActive()).isFalse();
        verify(vehicleRepository).save(vehicle);
    }
}
