package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.CreateAccountRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.UpdateAccountRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.AccountResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.BalanceResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.core.commands.finance.FinanceCommands.AccountFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateAccount;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateAccount;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final FinanceInPort finance;
    private final CurrentUserIdProvider currentUser;

    public AccountController(FinanceInPort finance, CurrentUserIdProvider currentUser) {
        this.finance = finance;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            Principal principal,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse response = AccountResponse.from(finance.createAccount(new CreateAccount(
                currentUser.get(principal), request.name(), request.type(), request.currency(), request.openingBalance()
        )));
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping
    public List<AccountResponse> list(
            Principal principal,
            @RequestParam(required = false) ResourceStatus status
    ) {
        return finance.listAccounts(new AccountFilter(currentUser.get(principal), status))
                .stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(Principal principal, @PathVariable UUID id) {
        return AccountResponse.from(finance.getAccount(currentUser.get(principal), id));
    }

    @PatchMapping("/{id}")
    public AccountResponse update(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return AccountResponse.from(finance.updateAccount(
                new UpdateAccount(currentUser.get(principal), id, request.name())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(Principal principal, @PathVariable UUID id) {
        finance.archiveAccount(currentUser.get(principal), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(Principal principal, @PathVariable UUID id) {
        return BalanceResponse.from(finance.getBalance(currentUser.get(principal), id));
    }
}
