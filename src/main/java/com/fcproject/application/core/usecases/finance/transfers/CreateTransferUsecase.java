package com.fcproject.application.core.usecases.finance.transfers;

import com.fcproject.application.core.commands.finance.FinanceCommands.Transfer;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferStatus;
import com.fcproject.application.ports.inbound.finance.CreateTransferInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireWritableAccount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.fingerprint;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeDescription;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeIdempotencyKey;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireDate;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateAmount;

public class CreateTransferUsecase implements CreateTransferInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CreateTransferUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public TransferResult transfer(Transfer command) {
        requireUser(command.userId());
        if (Objects.equals(command.sourceAccountId(), command.destinationAccountId())) {
            throw rule("SAME_TRANSFER_ACCOUNT", "Source and destination accounts must be different");
        }
        BigDecimal amount = validateAmount(command.amount());
        LocalDate effectiveDate = requireDate(command.effectiveDate());
        String description = normalizeDescription(command.description());
        String key = normalizeIdempotencyKey(command.idempotencyKey());
        String requestFingerprint = fingerprint(
                command.sourceAccountId(), command.destinationAccountId(), amount, effectiveDate, description
        );

        var existing = finance.findTransferByIdempotencyKey(command.userId(), key);
        if (existing.isPresent()) {
            if (requestFingerprint.equals(existing.get().transfer().requestFingerprint())) {
                return existing.get();
            }
            throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was already used with another payload");
        }

        Account source = requireWritableAccount(finance, command.userId(), command.sourceAccountId());
        Account destination = requireWritableAccount(finance, command.userId(), command.destinationAccountId());
        if (!source.currency().equals(destination.currency())) {
            throw rule("CURRENCY_MISMATCH", "Transfers between different currencies are not supported");
        }

        Instant now = clock.instant();
        UUID groupId = UUID.randomUUID();
        TransferGroup group = new TransferGroup(
                groupId, command.userId(), key, requestFingerprint, TransferStatus.COMPLETED,
                null, null, 0, now, now
        );
        FinancialEntry debit = transferEntry(
                command.userId(), source.id(), groupId, EntryType.TRANSFER_OUT,
                amount, effectiveDate, description, now
        );
        FinancialEntry credit = transferEntry(
                command.userId(), destination.id(), groupId, EntryType.TRANSFER_IN,
                amount, effectiveDate, description, now
        );
        return finance.saveTransfer(group, debit, credit);
    }

    private FinancialEntry transferEntry(
            UUID userId,
            UUID accountId,
            UUID groupId,
            EntryType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            Instant now
    ) {
        return new FinancialEntry(
                UUID.randomUUID(), userId, accountId, null, groupId, type, amount,
                EntryStatus.CLEARED, date, description, null, null, 0, null, now, now
        );
    }
}
