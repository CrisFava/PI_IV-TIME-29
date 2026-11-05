// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.enums.user;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VerificationStatus {
    VERIFIED, UNDER_REVIEW, REJECTED, PENDING, EXPIRED;
}
