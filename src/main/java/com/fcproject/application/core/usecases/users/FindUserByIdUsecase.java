package com.fcproject.application.core.usecases.users;

import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.inbound.userPorts.FindUserByIdInPort;
import com.fcproject.application.ports.outbound.UserOutPort;

import java.util.UUID;

public class FindUserByIdUsecase implements FindUserByIdInPort {
    private final UserOutPort repositoryOut;

    public FindUserByIdUsecase(UserOutPort repositoryOut) {
        this.repositoryOut = repositoryOut;
    }

    @Override
    public UserDomain execute(UUID id) {

        return repositoryOut.findById(id);
    }
}
