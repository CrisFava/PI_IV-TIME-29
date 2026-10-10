package com.converge.api.modules.user.presentation.mapper;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.presentation.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.presentation.dto.CreateUserResponseDTO;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    @DisplayName("Deve mapear CreateUserRequestDTO para entidade User inicializando valores padrão")
    void shouldMapRequestDtoToUserEntity() {
        AcademicRecord academicRecord = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br"
        );

        CreateUserRequestDTO dto = new CreateUserRequestDTO(
                "uid-firebase-999",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                "https://storage.firebase.com/foto.jpg",
                Set.of(ParticipationRole.PASSENGER),
                academicRecord,
                null,
                null
        );

        User user = mapper.toEntity(dto);

        assertThat(user).isNotNull();
        assertEquals("uid-firebase-999", user.getFirebaseUid());
        assertEquals("Ana Souza", user.getName());
        assertEquals("ana@puc-campinas.edu.br", user.getEmail());
        assertEquals("(19) 91234-5678", user.getPhone());
        assertThat(user.getParticipationRoles()).containsExactly(ParticipationRole.PASSENGER);
        assertEquals(academicRecord, user.getAcademicRecord());
        assertEquals(PaymentStanding.UP_TO_DATE, user.getPaymentStanding());
        assertEquals(0, user.getOverdueRidesCount());
        assertFalse(user.isDefaulter());
        assertTrue(user.isActive());
        assertThat(user.getReputation()).isNotNull();
    }

    @Test
    @DisplayName("Deve mapear entidade User para CreateUserResponseDTO preservando campos")
    void shouldMapUserEntityToResponseDto() {
        AcademicRecord academicRecord = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br"
        );

        User user = new User(
                "uid-firebase-999",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academicRecord
        );

        CreateUserResponseDTO responseDTO = mapper.toResponseDTO(user);

        assertThat(responseDTO).isNotNull();
        assertEquals("uid-firebase-999", responseDTO.firebaseUid());
        assertEquals("Ana Souza", responseDTO.name());
        assertEquals("ana@puc-campinas.edu.br", responseDTO.email());
        assertEquals(PaymentStanding.UP_TO_DATE, responseDTO.paymentStanding());
        assertFalse(responseDTO.isDefaulter());
        assertTrue(responseDTO.active());
    }
}
