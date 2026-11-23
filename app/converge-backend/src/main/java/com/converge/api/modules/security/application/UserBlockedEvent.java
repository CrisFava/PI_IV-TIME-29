// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.application;

/**
 * Um usuário bloqueou outro. Os outros módulos reagem (ex.: as caronas que eles
 * ainda iam fazer juntos deixam de ter os dois).
 */
public record UserBlockedEvent(String blockerId, String blockedId) {
}
