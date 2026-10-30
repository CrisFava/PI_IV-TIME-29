package com.converge.api.modules.user.application.dto;

public record UpdateUserCommand (
    String firebaseUid,
    UpdateUserRequestDTO request
) {
}
