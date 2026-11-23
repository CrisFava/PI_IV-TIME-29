// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import static com.converge.api.shared.infrastructure.config.customization.Color.*;

/**
 * Verificação de inicialização das variáveis de ambiente.
 *
 * Confere se a variável ENV_CHECK está definida no ambiente. Ela funciona como
 * uma "sentinela": se está presente, assume-se que o restante das variáveis de
 * ambiente (credenciais, URLs etc.) também foi carregado. Mostra o resultado no
 * log, em verde (sucesso) ou vermelho (falha), usando cores de terminal.
 */
@Slf4j
@Component
public class EnvChecker {

    /**
     * Retorna true se a variável de ambiente ENV_CHECK existe (qualquer valor,
     * até vazio) e false se não existe. Também registra no log a mensagem de
     * sucesso ("Environment variables loaded successfully.") ou de falha
     * ("Environment variables not found."), colorida, e restaura a cor do
     * terminal no final (RESET).
     * Por ser estático, é chamado diretamente (EnvChecker.check()), sem precisar
     * do bean do Spring.
     */
    public static boolean check(){

        boolean result = System.getenv("ENV_CHECK") != null;

        String msg = result  ? GREEN + "Environment variables loaded successfully."
                : RED + "Environment variables not found.";

        log.info("{}{}", msg, RESET);

        return result;
    }
}
