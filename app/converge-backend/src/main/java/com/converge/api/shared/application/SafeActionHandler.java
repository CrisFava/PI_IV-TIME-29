// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitário para executar ações secundárias e efeitos colaterais de forma resiliente,
 * capturando exceções e registrando logs apropriados sem interromper o fluxo principal.
 */
public final class SafeActionHandler {

    private static final Logger defaultLog = LoggerFactory.getLogger(SafeActionHandler.class);

    private SafeActionHandler() {
    }

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws Exception;
    }

    public static void run(String actionDescription, ThrowingAction action) {
        run(defaultLog, actionDescription, action);
    }

    public static void run(Logger logger, String actionDescription, ThrowingAction action) {
        try {
            action.run();
        } catch (Exception e) {
            Logger targetLogger = logger != null ? logger : defaultLog;
            targetLogger.error("Falha ao {}: {}", actionDescription, e.getMessage(), e);
        }
    }
}
