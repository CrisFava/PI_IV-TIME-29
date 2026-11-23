// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.enums;

/**
 * Tipos de ações sensíveis registradas na trilha de auditoria da plataforma.
 */
public enum AuditAction {
    PAYMENT_CONFIRMED,
    NO_SHOW_REGISTERED,
    RIDE_CANCELLED,
    USER_BLOCKED,
    REPORT_RESOLVED,
    ACADEMIC_VERIFIED,
    LICENSE_VERIFIED,
    USER_DEACTIVATED,
    RESERVED_SEAT_TERMINATED
}
