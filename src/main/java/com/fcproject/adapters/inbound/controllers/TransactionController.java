package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.CreateTransactionRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceRequests.UpdateTransactionRequest;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.PageResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.TransactionResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateEntry;
import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final FinanceInPort finance;
    private final CurrentUserIdProvider currentUser;

    public TransactionController(FinanceInPort finance, CurrentUserIdProvider currentUser) {
        this.finance = finance;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            Principal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        TransactionResponse response = TransactionResponse.from(finance.createEntry(new CreateEntry(
                currentUser.get(principal), request.accountId(), request.categoryId(), request.type(),
                request.amount(), request.status(), request.effectiveDate(), request.description(), idempotencyKey
        )));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(Principal principal, @PathVariable UUID id) {
        return TransactionResponse.from(finance.getEntry(currentUser.get(principal), id));
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            Principal principal,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(finance.listEntries(new EntryFilter(
                currentUser.get(principal), accountId, categoryId, type, status, from, to, page, size
        )));
    }

    @PatchMapping("/{id}")
    public TransactionResponse update(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        return TransactionResponse.from(finance.updateEntry(new UpdateEntry(
                currentUser.get(principal), id, request.accountId(), request.categoryId(), request.amount(),
                request.status(), request.effectiveDate(), request.description()
        )));
    }

    @PostMapping("/{id}/cancel")
    public TransactionResponse cancel(Principal principal, @PathVariable UUID id) {
        return TransactionResponse.from(finance.cancelEntry(currentUser.get(principal), id));
    }
}
