package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.TransferRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.TransferResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.core.commands.finance.FinanceCommands.Transfer;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final FinanceInPort finance;
    private final CurrentUserIdProvider currentUser;

    public TransferController(FinanceInPort finance, CurrentUserIdProvider currentUser) {
        this.finance = finance;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            Principal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferResponse response = TransferResponse.from(finance.transfer(new Transfer(
                currentUser.get(principal), request.sourceAccountId(), request.destinationAccountId(),
                request.amount(), request.effectiveDate(), request.description(), idempotencyKey
        )));
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + response.id())).body(response);
    }
}
