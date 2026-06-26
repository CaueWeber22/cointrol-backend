package com.fcproject.application.ports.inbound.userPorts;

import com.fcproject.application.core.domain.users.UserDomain;

public interface FindUserByEmailInPort {
    UserDomain execute(String email);
}
