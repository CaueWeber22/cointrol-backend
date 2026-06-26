package com.fcproject.application.ports.inbound.userPorts;

import com.fcproject.application.core.domain.users.UserDomain;

import java.util.UUID;

public interface FindUserByIdInPort {
    UserDomain execute(UUID id);
}
