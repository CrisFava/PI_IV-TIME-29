// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.startup;

import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.exception.ServerException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.converge.api.shared.infrastructure.config.customization.Color.*;

/**
 * Verificações de inicialização da API.
 *
 * Roda uma vez, quando o Spring cria este bean, e confere duas coisas: se as
 * variáveis de ambiente foram carregadas (EnvChecker) e se o servidor de socket
 * (usado pelo chat/tempo real) está no ar. Sem o servidor de socket, a API se
 * recusa a subir. Sem as variáveis de ambiente, ela sobe, mas avisa no log que
 * iniciou "com erros".
 */
@Slf4j
@Component
public class Setup {

    private static final boolean ONLY_BACKEND  = false;


    /**
     * Executa as verificações logo após o bean ser criado (@PostConstruct).
     * 1. Confere as variáveis de ambiente (EnvChecker.check) e guarda o resultado.
     * 2. Se o servidor de socket não estiver rodando, registra erro e lança
     *    ApplicationException (SERVER_NOT_RUNNING), o que derruba a inicialização
     *    da aplicação.
     * 3. Se as variáveis de ambiente estavam ok, loga "Initialized Successfully"
     *    em verde. Se não, loga "Initialized With Errors" em laranja, mas deixa
     *    a aplicação continuar.
     */
    @PostConstruct
    public void init()  {

        boolean isLoadedEnvVars = EnvChecker.check();

        if (!socketRunning()){
            log.error("{} Socket Server is not running.{}", RED, RESET);
            throw new ApplicationException(
                    ErrorCode.SERVER_NOT_RUNNING,
                    "Servidor não está no ar."
            );
        }

        if (isLoadedEnvVars) {
            log.info("{}Converge API Initialized Successfully.{}", GREEN, RESET);
            return;
        }

        log.warn("{}Converge API Initialized With Errors.{}", ORANGE, RESET);
    }

    /**
     * Diz se o servidor de socket está no ar, tentando abrir uma conexão de teste.
     * Se ONLY_BACKEND estiver ligado, devolve true sem testar. Caso contrário,
     * cria um SocketClient e devolve isConnected(); o try-with-resources fecha a
     * conexão no final. Se a conexão falhar com ServerException, devolve false.
     */
    private boolean socketRunning()  {
        if (Setup.ONLY_BACKEND) return true;

        try(SocketClient socket = new SocketClient();){
            return socket.isConnected();
        } catch (ServerException ex){
            return false;
        }
    }
}
