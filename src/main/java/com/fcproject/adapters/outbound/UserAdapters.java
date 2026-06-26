package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.mappers.UserMapper;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.outbound.UserOutPort;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class UserAdapters implements UserOutPort {

    private final UserJPARepository userRepository;


    public UserAdapters(UserJPARepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDomain findByEmail(String email) {

        log.info("Searching user by email: {}", email);
        return UserMapper.toDomain(userRepository.findByEmail(email).orElse(null));
    }

    @Override
    public void save(UserDomain user) {
        log.info("Saving user: {}", user);
        userRepository.save(UserMapper.toEntity(user));
    }


    @Override
    public UserDomain findById(UUID id) {

        log.info("Searching user by ID: {}", id);
        return UserMapper.toDomain(userRepository.findById(id).orElse(null));
    }
}
