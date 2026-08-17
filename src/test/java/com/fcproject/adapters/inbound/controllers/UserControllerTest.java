package com.fcproject.adapters.inbound.controllers;

import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.core.enums.Gender;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import com.fcproject.infrastructure.exceptions.GlobalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private SaveNewUserInPort saveNewUser;

    @Mock
    private FindUserByEmailInPort findUserByEmail;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new UserController(saveNewUser, findUserByEmail))
                .setControllerAdvice(new GlobalHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsAUserWithoutReturningPasswordData() throws Exception {
        UUID id = UUID.fromString("10000000-0000-0000-0000-000000000001");
        when(saveNewUser.execute(any())).thenReturn(new UserDomain(
                id,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "+55 11 99999-9999",
                Gender.FEMALE,
                LocalDate.of(1990, 1, 1)
        ));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "first_name": "Ada",
                                  "last_name": "Lovelace",
                                  "email": "ada@example.com",
                                  "phone": "+55 11 99999-9999",
                                  "gender": "female",
                                  "date_of_birth": "1990-01-01",
                                  "password": "Valid@123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/me"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsFutureBirthDate() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "first_name": "Ada",
                                  "last_name": "Lovelace",
                                  "email": "ada@example.com",
                                  "phone": "+55 11 99999-9999",
                                  "gender": "female",
                                  "date_of_birth": "2990-01-01",
                                  "password": "Valid@123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
    }
}
