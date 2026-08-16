package com.fcproject.application.core.usecases.users;

import com.fcproject.application.core.commands.CreateUserCommand;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.core.exceptions.RequiredFieldException;
import com.fcproject.application.core.exceptions.UserAlreadyExistsException;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import com.fcproject.application.ports.outbound.PasswordHasherOutPort;
import com.fcproject.application.ports.outbound.UserOutPort;

import java.time.LocalDate;

import static com.fcproject.application.core.utils.UserValidationUtil.normalizeEmail;
import static com.fcproject.application.core.utils.UserValidationUtil.passwordValidationUtil;

public class SaveNewUserUsecase implements SaveNewUserInPort {
    private final UserOutPort repositoryOut;
    private final PasswordHasherOutPort passwordHasher;

    public SaveNewUserUsecase(UserOutPort repositoryOut, PasswordHasherOutPort passwordHasher) {
        this.repositoryOut = repositoryOut;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserDomain execute(CreateUserCommand command) {
        if (command == null) {
            throw new RequiredFieldException("User data must be provided");
        }

        String normalizedEmail = normalizeEmail(command.email());
        userValidation(command, normalizedEmail);

        UserDomain user = new UserDomain(
                null,
                command.firstName().trim(),
                command.lastName().trim(),
                normalizedEmail,
                command.phone().trim(),
                command.gender(),
                command.dateOfBirth()
        );

        return repositoryOut.save(user, passwordHasher.hash(command.password()));
    }

    private void userValidation(CreateUserCommand command, String normalizedEmail) {
        passwordValidationUtil(command.password());

        requireNotBlank(command.firstName(), "First name");
        requireNotBlank(command.lastName(), "Last name");
        requireNotBlank(command.phone(), "Phone");
        if (command.gender() == null) {
            throw new RequiredFieldException("Gender must be provided");
        }
        if (command.dateOfBirth() == null) {
            throw new RequiredFieldException("Date of birth must be provided");
        }
        if (command.dateOfBirth().isAfter(LocalDate.now())) {
            throw new com.fcproject.application.core.exceptions.InvalidValueException("Date of birth cannot be in the future");
        }

        if (repositoryOut.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("A user with this e-mail already exists");
        }
    }


    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RequiredFieldException(fieldName + " must be filled");
        }
    }

}
