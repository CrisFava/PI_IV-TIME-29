package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.application.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteUserUseCase implements UseCase<String, Void> {

    private final UserUtils userUtils;
    private final UserRepository userRepository;

    @Override
    public Void execute(String firebaseUid) {
        User user = userUtils.getCurrentUser(firebaseUid);

        user.deactivate();
        userRepository.save(user);
        return null;
    }
}
