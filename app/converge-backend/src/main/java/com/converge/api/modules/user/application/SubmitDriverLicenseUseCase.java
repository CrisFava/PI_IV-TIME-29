// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.SubmitDriverLicenseCommand;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.application.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitDriverLicenseUseCase implements UseCase<SubmitDriverLicenseCommand, User> {
    
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override 
    public User execute(SubmitDriverLicenseCommand command) {
        User user = userUtils.getCurrentUser(command.firebaseUid());

        user.submitDriverLicense(userMapper.toDriverLicense(command.request()));

        return userRepository.save(user);
    }
}
