package com.fcproject.application.core.usecases.finance.entries;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.inbound.finance.CreateEntryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireWritableAccount;
import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireWritableCategory;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.fingerprint;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeDescription;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeIdempotencyKey;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireDate;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateAmount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateCategoryCompatibility;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validatePublicEntryType;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateWritableStatus;

public class CreateEntryUsecase implements CreateEntryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CreateEntryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public FinancialEntry createEntry(CreateEntry command) {
        requireUser(command.userId());
        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        validatePublicEntryType(command.type());
        BigDecimal amount = validateAmount(command.amount());
        EntryStatus status = validateWritableStatus(command.status());
        LocalDate effectiveDate = requireDate(command.effectiveDate());
        String description = normalizeDescription(command.description());
        String fingerprint = fingerprint(
                command.accountId(), command.categoryId(), command.type(), amount,
                status, effectiveDate, description
        );

        var existing = finance.findEntryByIdempotencyKey(command.userId(), idempotencyKey);
        if (existing.isPresent()) {
            if (fingerprint.equals(existing.get().requestFingerprint())) {
                return existing.get();
            }
            throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was already used with another payload");
        }

        Account account = requireWritableAccount(finance, command.userId(), command.accountId());
        Category category = requireWritableCategory(finance, command.userId(), command.categoryId());
        validateCategoryCompatibility(command.type(), category);
        Instant now = clock.instant();
        return finance.saveEntry(new FinancialEntry(
                UUID.randomUUID(), command.userId(), account.id(), category.id(), null,
                command.type(), amount, status, effectiveDate, description,
                idempotencyKey, fingerprint, 0, null, now, now
        ));
    }
}
