package com.fcproject.application.core.usecases.users;

import com.fcproject.application.core.commands.CreateUserCommand;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.core.enums.Gender;
import com.fcproject.application.core.exceptions.UserAlreadyExistsException;
import com.fcproject.application.ports.outbound.PasswordHasherOutPort;
import com.fcproject.application.ports.outbound.UserOutPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveNewUserUsecaseTest {
    @Mock
    private UserOutPort users;

    @Mock
    private PasswordHasherOutPort passwordHasher;

    private SaveNewUserUsecase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveNewUserUsecase(users, passwordHasher);
    }

    @Test
    void normalizesEmailAndHashesPasswordBeforePersisting() {
        CreateUserCommand command = command("  User@Example.COM ");
        when(users.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordHasher.hash("Valid@123")).thenReturn("bcrypt-hash");
        when(users.save(any(UserDomain.class), eq("bcrypt-hash"))).thenAnswer(invocation -> {
            UserDomain input = invocation.getArgument(0);
            return new UserDomain(
                    UUID.randomUUID(),
                    input.getFirstName(),
                    input.getLastName(),
                    input.getEmail(),
                    input.getPhone(),
                    input.getGender(),
                    input.getDateOfBirth()
            );
        });

        UserDomain saved = useCase.execute(command);

        ArgumentCaptor<UserDomain> userCaptor = ArgumentCaptor.forClass(UserDomain.class);
        verify(users).save(userCaptor.capture(), eq("bcrypt-hash"));
        verify(passwordHasher).hash("Valid@123");
        assertEquals("user@example.com", userCaptor.getValue().getEmail());
        assertEquals("user@example.com", saved.getEmail());
    }

    @Test
    void rejectsDuplicatedNormalizedEmailBeforeHashing() {
        when(users.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> useCase.execute(command(" USER@example.com ")));

        verify(passwordHasher, never()).hash(any());
        verify(users, never()).save(any(), any());
    }

    private CreateUserCommand command(String email) {
        return new CreateUserCommand(
                "Ada",
                "Lovelace",
                email,
                "+55 11 99999-9999",
                Gender.FEMALE,
                LocalDate.of(1990, 1, 1),
                "Valid@123"
        );
    }
}
