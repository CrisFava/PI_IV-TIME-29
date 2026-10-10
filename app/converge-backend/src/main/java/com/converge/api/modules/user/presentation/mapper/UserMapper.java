package com.converge.api.modules.user.presentation.mapper;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.presentation.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.presentation.dto.CreateUserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new User(
                dto.firebaseUid(),
                dto.name(),
                dto.email(),
                dto.phone(),
                dto.photoUrl(),
                dto.participationRoles(),
                dto.academicRecord(),
                dto.driverLicense(),
                dto.pixKey()
        );
    }

    public CreateUserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            // TODO: Throw and Treat the error
            return null;
        }

        return new CreateUserResponseDTO(
                user.getId() != null ? user.getId().toHexString() : null,
                user.getFirebaseUid(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPhotoUrl(),
                user.getParticipationRoles(),
                user.getAcademicRecord(),
                user.getDriverLicense(),
                user.getPixKey(),
                user.getPaymentStanding(),
                user.getOverdueRidesCount(),
                user.isDefaulter(),
                user.getDefaulterSince(),
                user.getReputation(),
                user.getBadges(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
