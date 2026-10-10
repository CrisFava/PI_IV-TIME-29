package com.converge.api.modules.user.domain.enums.user;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VerificationStatus {
    VERIFIED, UNDER_REVIEW, REJECTED, PENDING, EXPIRED;
}
