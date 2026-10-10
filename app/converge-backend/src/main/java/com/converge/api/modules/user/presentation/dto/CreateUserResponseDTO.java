package com.converge.api.modules.user.presentation.dto;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.DriverLicense;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.UserBadge;

import java.time.LocalDateTime;
import java.util.Set;

public record CreateUserResponseDTO(
        String id,
        String firebaseUid,
        String name,
        String email,
        String phone,
        String photoUrl,
        Set<ParticipationRole> participationRoles,
        AcademicRecord academicRecord,
        DriverLicense driverLicense,
        String pixKey,
        PaymentStanding paymentStanding,
        short overdueRidesCount,
        boolean isDefaulter,
        LocalDateTime defaulterSince,
        Reputation reputation,
        Set<UserBadge> badges,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
