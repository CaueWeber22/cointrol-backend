package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.users.RoleEntity;
import com.fcproject.adapters.outbound.entities.users.UserEntity;
import com.fcproject.adapters.outbound.mappers.UserMapper;
import com.fcproject.adapters.outbound.persistence.RoleJPARepository;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.outbound.UserOutPort;

import java.util.Optional;
import java.util.UUID;

public class UserAdapters implements UserOutPort {
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserJPARepository userRepository;
    private final RoleJPARepository roleRepository;

    public UserAdapters(UserJPARepository userRepository, RoleJPARepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<UserDomain> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDomain save(UserDomain user, String passwordHash) {
        UserEntity entity = UserMapper.toEntity(user, passwordHash);
        RoleEntity defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER is not configured"));
        entity.addRole(defaultRole);
        return UserMapper.toDomain(userRepository.save(entity));
    }

    @Override
    public Optional<UserDomain> findById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }
}
