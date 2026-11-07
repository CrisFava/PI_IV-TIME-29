// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.CpfValidationRequestDTO;
import com.converge.api.modules.user.application.dto.CpfValidationResponseDTO;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.cpf.ValidateCpfHandler;
import com.converge.socket.handler.cpf.ValidateCpfRequest;
import com.converge.socket.handler.cpf.ValidateCpfResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ValidateCpfUseCase implements UseCase<CpfValidationRequestDTO, CpfValidationResponseDTO> {

    @Override
    public CpfValidationResponseDTO execute(CpfValidationRequestDTO request) {
        if (request == null || request.cpf() == null) {
            return new CpfValidationResponseDTO(false, null, "CPF não pode ser nulo.");
        }

        // Tenta validar através do Socket Server TCP
        try (SocketClient client = new SocketClient()) {
            Message response = client.send(new ValidateCpfRequest(request.cpf()));
            if (response instanceof ValidateCpfResponse cpfResp) {
                return new CpfValidationResponseDTO(
                        cpfResp.isValid(),
                        cpfResp.getFormattedCpf(),
                        cpfResp.getMessage()
                );
            }
        } catch (Exception e) {
            log.warn("Socket Server indisponível para validação de CPF ({}), executando validação local de contingência.", e.getMessage());
        }

        // Fallback local caso o servidor socket não esteja ativo no momento
        try {
            ValidateCpfHandler localHandler = new ValidateCpfHandler();
            ValidateCpfResponse localResp = localHandler.execute(new ValidateCpfRequest(request.cpf()));
            return new CpfValidationResponseDTO(
                    localResp.isValid(),
                    localResp.getFormattedCpf(),
                    localResp.getMessage()
            );
        } catch (Exception e) {
            return new CpfValidationResponseDTO(false, null, "Erro na validação do CPF: " + e.getMessage());
        }
    }
}
