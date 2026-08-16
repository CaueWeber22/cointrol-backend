package com.fcproject.adapters.outbound.mappers;

import com.fcproject.adapters.outbound.entities.users.UserEntity;
import com.fcproject.application.core.domain.users.UserDomain;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDomain toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserDomain(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getGender(),
                entity.getDateOfBirth()
        );
    }

    public static UserEntity toEntity(UserDomain domain, String passwordHash) {
        if (domain == null) {
            throw new IllegalArgumentException("User domain must be provided");
        }

        return new UserEntity(
                domain.getFirstName(),
                domain.getLastName(),
                domain.getEmail(),
                domain.getPhone(),
                domain.getGender(),
                domain.getDateOfBirth(),
                passwordHash
        );
    }
}
