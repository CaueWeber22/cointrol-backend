package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.request.UserCreationRequest;
import com.fcproject.adapters.inbound.dto.response.UserInfoResponse;
import com.fcproject.application.core.commands.CreateUserCommand;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.core.enums.Gender;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final SaveNewUserInPort saveNewUser;
    private final FindUserByEmailInPort findUserByEmail;

    public UserController(
            SaveNewUserInPort saveNewUser,
            FindUserByEmailInPort findUserByEmail
    ) {
        this.saveNewUser = saveNewUser;
        this.findUserByEmail = findUserByEmail;
    }

    @PostMapping
    public ResponseEntity<UserInfoResponse> create(@Valid @RequestBody UserCreationRequest request) {
        CreateUserCommand command = new CreateUserCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                Gender.fromString(request.gender()),
                request.dateOfBirth(),
                request.password()
        );
        UserDomain created = saveNewUser.execute(command);
        return ResponseEntity.created(URI.create("/api/v1/users/me"))
                .body(UserInfoResponse.from(created));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(Principal principal) {
        return ResponseEntity.ok(UserInfoResponse.from(findUserByEmail.execute(principal.getName())));
    }
}
