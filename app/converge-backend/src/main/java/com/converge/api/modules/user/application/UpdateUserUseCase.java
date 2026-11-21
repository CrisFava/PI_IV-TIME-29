// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import org.springframework.stereotype.Service;

import com.converge.api.modules.user.application.dto.UpdateUserRequestDTO;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.modules.user.application.dto.UpdateUserCommand;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class UpdateUserUseCase implements UseCase<UpdateUserCommand, User>{
    
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    private final ValidatePixCpfUseCase validatePixCpf;

    // Chave PIX do tipo CPF: quem confere os dígitos é o servidor
    private void validatePixKey(String pixKey, PixKeyType pixKeyType) {
        if (pixKeyType == PixKeyType.CPF && pixKey != null && !pixKey.isBlank()) {
            validatePixCpf.execute(pixKey);
        }
    }

    @Override
    public User execute (UpdateUserCommand command) {
        
        User user = userUtils.getCurrentUser(command.firebaseUid());

        UpdateUserRequestDTO request = command.request();
        validatePixKey(request.pixKey(), request.pixKeyType());
        user.updateProfile(
            request.name(),
            request.phone(),
            request.photoUrl(),
            request.pixKey(),
            request.pixKeyType()
        );

        return userRepository.save(user);


    }

}
