// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.CreateVehicleUseCase;
import com.converge.api.modules.user.application.DeleteVehicleUseCase;
import com.converge.api.modules.user.application.GetVehicleUseCase;
import com.converge.api.modules.user.application.UpdateVehicleUseCase;
import com.converge.api.modules.user.application.ValidateRenavamUseCase;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.application.dto.CreateVehicleCommand;
import com.converge.api.modules.user.application.dto.CreateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.FuelSourceRequestDTO;
import com.converge.api.modules.user.application.dto.UpdateVehicleCommand;
import com.converge.api.modules.user.application.dto.UpdateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.VehicleRefCommand;
import com.converge.api.modules.user.application.gateway.RenavamValidationGateway;
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
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.handler.renavam.ValidateRenavamHandler;
import com.converge.socket.handler.renavam.ValidateRenavamRequest;
import com.converge.socket.handler.renavam.ValidateRenavamResponse;
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
    private ValidateRenavamUseCase validateRenavam;
    private final ObjectId userObjectId = new ObjectId();

    @BeforeEach
    void setUp() {
        mapper = new VehicleMapper();
        vehicleAccess = new VehicleAccess(vehicleRepository, userUtils);
        validateRenavam = new ValidateRenavamUseCase(localRenavamGateway());
    }

    private CreateVehicleRequestDTO validRequest() {
        return new CreateVehicleRequestDTO(
                "Gol Azul",
                "Volkswagen Gol",
                "abc-1234",
                "14283256656",
                "Azul",
                VehicleType.HATCH,
                5,
                List.of(new FuelSourceRequestDTO(EnergySource.GASOLINE, 12.0, new BigDecimal("5.89")))
        );
    }

    // Mesma regra do servidor, rodando o handler direto: o teste não depende de socket.
    private RenavamValidationGateway localRenavamGateway() {
        return renavam -> {
            try {
                ValidateRenavamResponse response = new ValidateRenavamHandler().execute(new ValidateRenavamRequest(renavam));
                return new RenavamValidationGateway.Result(response.isValid(), response.getNormalizedRenavam(), response.getMessage());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private CreateVehicleRequestDTO requestWithRenavam(String renavam) {
        CreateVehicleRequestDTO base = validRequest();
        return new CreateVehicleRequestDTO(base.nickname(), base.model(), base.licensePlate(), renavam,
                base.color(), base.type(), base.totalCapacity(), base.fuelSources());
    }

    private UpdateVehicleRequestDTO updateRequestWithRenavam(String renavam) {
        return new UpdateVehicleRequestDTO(
                "Gol Azul",
                "Volkswagen Gol",
                "ABC1234",
                renavam,
                "Azul",
                VehicleType.HATCH,
                5,
                validRequest().fuelSources()
        );
    }

    private void assertRenavamError(Runnable action, String message) {
        ApplicationException error = assertThrows(ApplicationException.class, action::run);
        assertThat(error.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(error.getError().message()).isEqualTo(message);
        assertThat(error.getError().fields()).containsExactly("renavam");
    }
    private Vehicle ownedVehicle(String ownerId) {
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
    @DisplayName("Quem não é motorista não pode cadastrar veículo")
    void shouldRejectCreationWhenUserIsNotDriver() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(false);

        assertThrows(ApplicationException.class,
                () -> createVehicle.execute(new CreateVehicleCommand(UID, validRequest())));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motorista deve cadastrar veículo com a placa normalizada")
    void shouldCreateVehicleForDriver() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
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
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
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
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
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

    @Test
    @DisplayName("Cadastro com RENAVAM de dígito verificador errado deve ser recusado sem salvar")
    void shouldRejectCreationWithWrongCheckDigit() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);

        assertRenavamError(
                () -> createVehicle.execute(new CreateVehicleCommand(UID, requestWithRenavam("14283256657"))),
                "RENAVAM inválido.");
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cadastro com RENAVAM de tamanho errado ou dígitos repetidos deve ser recusado")
    void shouldRejectCreationWithWrongLengthOrRepeatedDigits() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);

        assertRenavamError(
                () -> createVehicle.execute(new CreateVehicleCommand(UID, requestWithRenavam("1428325665"))),
                "O RENAVAM deve ter 11 dígitos.");
        assertRenavamError(
                () -> createVehicle.execute(new CreateVehicleCommand(UID, requestWithRenavam("11111111111"))),
                "O RENAVAM deve ter 11 dígitos.");
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cadastro deve guardar o RENAVAM só com os dígitos")
    void shouldStoreRenavamOnlyWithDigits() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findByUserIdAndLicensePlate(userObjectId.toHexString(), "ABC1234")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle created = createVehicle.execute(new CreateVehicleCommand(UID, requestWithRenavam("142.832.566-56")));

        assertThat(created.getRenavam()).isEqualTo("14283256656");
    }

    @Test
    @DisplayName("Reativação com RENAVAM inválido deve ser recusada sem alterar o veículo antigo")
    void shouldRejectReactivationWithInvalidRenavam() {
        CreateVehicleUseCase createVehicle = new CreateVehicleUseCase(userUtils, vehicleRepository, mapper, validateRenavam);
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.hasRole(ParticipationRole.DRIVER)).thenReturn(true);

        assertRenavamError(
                () -> createVehicle.execute(new CreateVehicleCommand(UID, requestWithRenavam("14283256657"))),
                "RENAVAM inválido.");
        verify(vehicleRepository, never()).findByUserIdAndLicensePlate(any(), any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Edição deve guardar o RENAVAM só com os dígitos")
    void shouldStoreNormalizedRenavamOnUpdate() {
        UpdateVehicleUseCase updateVehicle = new UpdateVehicleUseCase(vehicleAccess, vehicleRepository, mapper, validateRenavam);
        ObjectId vehicleId = new ObjectId();
        Vehicle vehicle = ownedVehicle(userObjectId.toHexString());
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle updated = updateVehicle.execute(
                new UpdateVehicleCommand(vehicleId.toHexString(), UID, updateRequestWithRenavam("142.832.566-56")));

        assertThat(updated.getRenavam()).isEqualTo("14283256656");
    }

    @Test
    @DisplayName("Edição com RENAVAM inválido deve ser recusada sem salvar nem alterar o veículo")
    void shouldRejectUpdateWithInvalidRenavam() {
        UpdateVehicleUseCase updateVehicle = new UpdateVehicleUseCase(vehicleAccess, vehicleRepository, mapper, validateRenavam);
        ObjectId vehicleId = new ObjectId();
        Vehicle vehicle = ownedVehicle(userObjectId.toHexString());
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(userObjectId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        assertRenavamError(
                () -> updateVehicle.execute(
                        new UpdateVehicleCommand(vehicleId.toHexString(), UID, updateRequestWithRenavam("14283256657"))),
                "RENAVAM inválido.");
        assertThat(vehicle.getRenavam()).isEqualTo("14283256656");
        verify(vehicleRepository, never()).save(any());
    }
}
