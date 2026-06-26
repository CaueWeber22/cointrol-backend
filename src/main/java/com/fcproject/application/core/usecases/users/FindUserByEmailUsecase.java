package com.fcproject.application.core.usecases.users;

import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import com.fcproject.application.ports.outbound.UserOutPort;

import static com.fcproject.application.core.utils.UserValidationUtil.emailValidationUtil;

public class FindUserByEmailUsecase implements FindUserByEmailInPort {
    private final UserOutPort userOutPort;

    public FindUserByEmailUsecase(UserOutPort userOutPort) {
        this.userOutPort = userOutPort;
    }

    @Override
    public UserDomain execute(String email) {
        emailValidationUtil(email);
        email = email.trim().toLowerCase();

        return userOutPort.findByEmail(email);
    }

}
