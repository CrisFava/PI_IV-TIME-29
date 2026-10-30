package com.converge.api.modules.user.application.mapper;

import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.modules.user.application.dto.AcademicRecordRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserResponseDTO;
import com.converge.api.modules.user.application.dto.DriverLicenseRequestDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;


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
                toAcademicRecord(dto.academicRecord()),
                toDriverLicense(dto.driverLicense()),
                dto.pixKey(),
                dto.pixKeyType()
        );
    }

    public CreateUserResponseDTO toResponse(User user) {
        if (user == null) {
            throw new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado."
             );
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
                user.getPixKeyType(),
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

    public AcademicRecord toAcademicRecord(AcademicRecordRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new AcademicRecord(
                dto.university(),
                dto.course(),
                dto.enrollmentNumber(),
                dto.institutionalEmail(),
                dto.documentUrl(),
                VerificationStatus.PENDING,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                null,
                null,
                null
        );
    }

    public DriverLicense toDriverLicense(DriverLicenseRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return new DriverLicense(
            dto.number(),
            dto.category(),
            dto.expiresAt(),
            dto.documentUrl()
        );
    }
}
