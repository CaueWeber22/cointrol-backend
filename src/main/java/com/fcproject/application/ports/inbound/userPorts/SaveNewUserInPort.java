package com.fcproject.application.ports.inbound.userPorts;

import com.fcproject.application.core.domain.users.UserDomain;

public interface SaveNewUserInPort {
    public void execute(UserDomain user);
}
