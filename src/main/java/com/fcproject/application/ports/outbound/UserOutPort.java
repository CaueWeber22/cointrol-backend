package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.users.UserDomain;

import java.util.UUID;

public interface UserOutPort {
        UserDomain findByEmail(String email);

        void save(UserDomain user);

        UserDomain findById(UUID id);
}
