package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.finance.CategoryEntity;
import com.fcproject.adapters.outbound.entities.users.RoleEntity;
import com.fcproject.adapters.outbound.entities.users.UserEntity;
import com.fcproject.adapters.outbound.mappers.UserMapper;
import com.fcproject.adapters.outbound.persistence.finance.CategoryJPARepository;
import com.fcproject.adapters.outbound.persistence.RoleJPARepository;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.application.core.domain.finance.DefaultCategoryCatalog.DefaultCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySource;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.ports.outbound.UserOutPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserAdapters implements UserOutPort {
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserJPARepository userRepository;
    private final RoleJPARepository roleRepository;
    private final CategoryJPARepository categoryRepository;
    private final Clock clock;

    public UserAdapters(
            UserJPARepository userRepository,
            RoleJPARepository roleRepository,
            CategoryJPARepository categoryRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
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
        return saveWithDefaultCategories(user, passwordHash, List.of());
    }

    @Override
    @Transactional
    public UserDomain saveWithDefaultCategories(
            UserDomain user,
            String passwordHash,
            List<DefaultCategory> defaultCategories
    ) {
        UserEntity entity = UserMapper.toEntity(user, passwordHash);
        RoleEntity defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER is not configured"));
        entity.addRole(defaultRole);
        UserEntity saved = userRepository.saveAndFlush(entity);
        saveMissingDefaultCategories(saved.getId(), defaultCategories);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<UserDomain> findById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }

    private void saveMissingDefaultCategories(UUID userId, List<DefaultCategory> defaultCategories) {
        if (defaultCategories.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        categoryRepository.saveAll(defaultCategories.stream()
                .filter(defaultCategory -> !categoryRepository.existsName(
                        userId, defaultCategory.kind(), defaultCategory.name()
                ))
                .map(defaultCategory -> new CategoryEntity(
                        UUID.randomUUID(), userId, defaultCategory.name(), defaultCategory.kind(),
                        ResourceStatus.ACTIVE, CategorySource.DEFAULT, defaultCategory.key(), 0, now, now
                ))
                .toList());
    }
}
