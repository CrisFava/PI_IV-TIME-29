package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.application.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;


@Service
@RequiredArgsConstructor
public class BecomeDriverUseCase implements UseCase<String, User> {

    private final UserUtils userUtils;
    private final UserRepository userRepository;

    @Override
    public User execute(String firebaseUid) {
        User user = userUtils.getCurrentUser(firebaseUid);

        user.addRole(ParticipationRole.DRIVER);            
        return userRepository.save(user);
    }
}

