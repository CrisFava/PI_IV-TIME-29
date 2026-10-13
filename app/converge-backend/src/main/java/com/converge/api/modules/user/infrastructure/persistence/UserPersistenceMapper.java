package com.converge.api.modules.user.infrastructure.persistence;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.presentation.dto.CreateUserRequestDTO;

public class UserPersistenceMapper {
    public User toEntity(CreateUserRequestDTO dto) {
        if (dto == null) {
            // TODO: Throw and Treat the error
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
}
