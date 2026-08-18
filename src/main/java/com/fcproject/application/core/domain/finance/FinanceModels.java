package com.fcproject.application.core.domain.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public final class FinanceModels {
    private FinanceModels() {
    }

    public enum AccountType { CHECKING, SAVINGS, CASH, INVESTMENT }

    public enum ResourceStatus { ACTIVE, ARCHIVED }

    public enum CategoryKind { INCOME, EXPENSE }

    public enum EntryType { INCOME, EXPENSE, OPENING_BALANCE, TRANSFER_IN, TRANSFER_OUT }

    public enum EntryStatus { PENDING, CLEARED, CANCELED }

    public enum TransferStatus { COMPLETED, CANCELED }

    public record Account(
            UUID id,
            UUID userId,
            String name,
            AccountType type,
            String currency,
            ResourceStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Category(
            UUID id,
            UUID userId,
            String name,
            CategoryKind kind,
            ResourceStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record FinancialEntry(
            UUID id,
            UUID userId,
            UUID accountId,
            UUID categoryId,
            UUID transferGroupId,
            EntryType type,
            BigDecimal amount,
            EntryStatus status,
            LocalDate effectiveDate,
            String description,
            String idempotencyKey,
            String requestFingerprint,
            long version,
            Instant canceledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TransferGroup(
            UUID id,
            UUID userId,
            String idempotencyKey,
            String requestFingerprint,
            TransferStatus status,
            String cancelReason,
            Instant canceledAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TransferResult(
            TransferGroup transfer,
            FinancialEntry debit,
            FinancialEntry credit
    ) {
    }

    public record AccountBalance(
            UUID accountId,
            String currency,
            BigDecimal cleared,
            BigDecimal pending,
            BigDecimal projected
    ) {
    }

    public record CurrencySummary(
            String currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal net
    ) {
    }

    public record CategorySummary(
            UUID categoryId,
            String categoryName,
            CategoryKind kind,
            String currency,
            BigDecimal amount
    ) {
    }

    public record MonthlySummary(
            YearMonth month,
            String currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal net
    ) {
    }

    public record PageResult<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public PageResult {
            content = List.copyOf(content);
        }
    }
}
