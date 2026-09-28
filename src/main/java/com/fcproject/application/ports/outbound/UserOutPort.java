package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.finance.DefaultCategoryCatalog.DefaultCategory;
import com.fcproject.application.core.domain.users.UserDomain;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface UserOutPort {
        Optional<UserDomain> findByEmail(String email);

        boolean existsByEmail(String email);

        UserDomain save(UserDomain user, String passwordHash);

        UserDomain saveWithDefaultCategories(
                UserDomain user,
                String passwordHash,
                List<DefaultCategory> defaultCategories
        );

        Optional<UserDomain> findById(UUID id);
}
