package com.fcproject.adapters.inbound.security;

import com.fcproject.application.core.exceptions.BusinessRuleException;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
public class CurrentUserIdProvider {
    private final FindUserByEmailInPort findUserByEmail;

    public CurrentUserIdProvider(FindUserByEmailInPort findUserByEmail) {
        this.findUserByEmail = findUserByEmail;
    }

    public UUID get(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BusinessRuleException("USER_REQUIRED", "Authenticated user is required");
        }
        return findUserByEmail.execute(principal.getName()).getId();
    }
}
