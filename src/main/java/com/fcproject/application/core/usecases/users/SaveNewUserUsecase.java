package com.fcproject.application.core.usecases.users;

import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import com.fcproject.application.ports.outbound.UserOutPort;
import com.fcproject.infrastructure.exceptions.global.NotAllFieldsFilledException;
import com.fcproject.infrastructure.exceptions.user.UserAlreadyExistsException;

import static com.fcproject.application.core.utils.UserValidationUtil.emailValidationUtil;
import static com.fcproject.application.core.utils.UserValidationUtil.passwordValidationUtil;

public class SaveNewUserUsecase implements SaveNewUserInPort {
    private final UserOutPort repositoryOut;

    public SaveNewUserUsecase(UserOutPort repositoryOut) {
        this.repositoryOut = repositoryOut;
    }

    @Override
    public void execute(UserDomain user) {

        userValidation(user);
        user.setEmail(user.getEmail().trim().toLowerCase());

        repositoryOut.save(user);
    }

    public void userValidation(UserDomain user) {
        emailValidationUtil(user.getEmail());
        passwordValidationUtil(user.getPassword());

        requireNotBlank(user.getFirstName(), "First Name");
        requireNotBlank(user.getLastName(), "Last Name");
        requireNotBlank(user.getPhone(), "Phone");

        if (repositoryOut.findByEmail(user.getEmail()) != null) {
                throw new UserAlreadyExistsException("An user with this e-mail already exists");
        }
    }


    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new NotAllFieldsFilledException(fieldName + " must be filled");
        }
    }

}
