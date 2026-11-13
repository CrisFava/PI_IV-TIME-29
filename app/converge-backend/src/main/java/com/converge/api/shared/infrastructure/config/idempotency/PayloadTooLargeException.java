// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import java.io.IOException;

/**
 * Sinaliza que o corpo da requisição excedeu o limite máximo configurado para cache em memória.
 */
final class PayloadTooLargeException extends IOException {
}
