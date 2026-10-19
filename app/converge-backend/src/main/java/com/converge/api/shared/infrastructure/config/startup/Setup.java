package com.converge.api.shared.infrastructure.config.startup;

import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.exception.ServerException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.converge.api.shared.infrastructure.config.customization.Color.*;

@Slf4j
@Component
public class Setup {

    private static final boolean ONLY_BACKEND  = true;


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

    private boolean socketRunning()  {
        if (Setup.ONLY_BACKEND) return true;

        try(SocketClient socket = new SocketClient();){
            return socket.isConnected();
        } catch (ServerException ex){
            return false;
        }
    }
}
