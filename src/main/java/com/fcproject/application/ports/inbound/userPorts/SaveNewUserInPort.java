package com.fcproject.application.ports.inbound.userPorts;

import com.fcproject.application.core.commands.CreateUserCommand;
import com.fcproject.application.core.domain.users.UserDomain;

public interface SaveNewUserInPort {
    UserDomain execute(CreateUserCommand command);
}
