package com.fcproject.application.core.commands.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.AccountType;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceCommands {
    private FinanceCommands() {
    }

    public record CreateAccount(
            UUID userId,
            String name,
            AccountType type,
            String currency,
            BigDecimal openingBalance
    ) {
    }

    public record UpdateAccount(UUID userId, UUID accountId, String name) {
    }

    public record CreateCategory(UUID userId, String name, CategoryKind kind) {
    }

    public record UpdateCategory(UUID userId, UUID categoryId, String name) {
    }

    public record CreateEntry(
            UUID userId,
            UUID accountId,
            UUID categoryId,
            EntryType type,
            BigDecimal amount,
            EntryStatus status,
            LocalDate effectiveDate,
            String description,
            String idempotencyKey
    ) {
    }

    public record UpdateEntry(
            UUID userId,
            UUID entryId,
            UUID accountId,
            UUID categoryId,
            BigDecimal amount,
            EntryStatus status,
            LocalDate effectiveDate,
            String description
    ) {
    }

    public record EntryFilter(
            UUID userId,
            UUID accountId,
            UUID categoryId,
            EntryType type,
            EntryStatus status,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
    }

    public record Transfer(
            UUID userId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            LocalDate effectiveDate,
            String description,
            String idempotencyKey
    ) {
    }

    public record AccountFilter(UUID userId, ResourceStatus status) {
    }
}
