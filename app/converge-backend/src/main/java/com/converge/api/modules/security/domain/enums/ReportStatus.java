// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.domain.enums;

// Situação da denúncia. Hoje só PENDING é gravado: a moderação ainda não existe.
public enum ReportStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED,
    ARCHIVED
}
