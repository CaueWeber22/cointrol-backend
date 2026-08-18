package com.fcproject.adapters.inbound.dto.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.AccountBalance;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.CurrencySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.MonthlySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceResponses {
    private FinanceResponses() {
    }

    public record AccountResponse(
            UUID id,
            String name,
            String type,
            String currency,
            String status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static AccountResponse from(Account value) {
            return new AccountResponse(
                    value.id(), value.name(), value.type().name(), value.currency(), value.status().name(),
                    value.version(), value.createdAt(), value.updatedAt()
            );
        }
    }

    public record CategoryResponse(
            UUID id,
            String name,
            String kind,
            String status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CategoryResponse from(Category value) {
            return new CategoryResponse(
                    value.id(), value.name(), value.kind().name(), value.status().name(), value.version(),
                    value.createdAt(), value.updatedAt()
            );
        }
    }

    public record TransactionResponse(
            UUID id,
            UUID accountId,
            UUID categoryId,
            UUID transferGroupId,
            String type,
            BigDecimal amount,
            String status,
            LocalDate effectiveDate,
            String description,
            long version,
            Instant canceledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TransactionResponse from(FinancialEntry value) {
            return new TransactionResponse(
                    value.id(), value.accountId(), value.categoryId(), value.transferGroupId(), value.type().name(),
                    value.amount(), value.status().name(), value.effectiveDate(), value.description(),
                    value.version(), value.canceledAt(), value.createdAt(), value.updatedAt()
            );
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static PageResponse<TransactionResponse> from(PageResult<FinancialEntry> value) {
            return new PageResponse<>(
                    value.content().stream().map(TransactionResponse::from).toList(),
                    value.page(), value.size(), value.totalElements(), value.totalPages()
            );
        }
    }

    public record BalanceResponse(
            UUID accountId,
            String currency,
            BigDecimal cleared,
            BigDecimal pending,
            BigDecimal projected
    ) {
        public static BalanceResponse from(AccountBalance value) {
            return new BalanceResponse(
                    value.accountId(), value.currency(), value.cleared(), value.pending(), value.projected()
            );
        }
    }

    public record TransferResponse(
            UUID id,
            String status,
            String cancelReason,
            Instant canceledAt,
            TransactionResponse debit,
            TransactionResponse credit,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TransferResponse from(TransferResult value) {
            return new TransferResponse(
                    value.transfer().id(), value.transfer().status().name(), value.transfer().cancelReason(),
                    value.transfer().canceledAt(), TransactionResponse.from(value.debit()),
                    TransactionResponse.from(value.credit()), value.transfer().version(),
                    value.transfer().createdAt(), value.transfer().updatedAt()
            );
        }
    }

    public record CurrencySummaryResponse(
            String currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal net
    ) {
        public static CurrencySummaryResponse from(CurrencySummary value) {
            return new CurrencySummaryResponse(value.currency(), value.income(), value.expenses(), value.net());
        }
    }

    public record CategorySummaryResponse(
            UUID categoryId,
            String categoryName,
            String kind,
            String currency,
            BigDecimal amount
    ) {
        public static CategorySummaryResponse from(CategorySummary value) {
            return new CategorySummaryResponse(
                    value.categoryId(), value.categoryName(), value.kind().name(), value.currency(), value.amount()
            );
        }
    }

    public record MonthlySummaryResponse(
            String month,
            String currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal net
    ) {
        public static MonthlySummaryResponse from(MonthlySummary value) {
            return new MonthlySummaryResponse(
                    value.month().toString(), value.currency(), value.income(), value.expenses(), value.net()
            );
        }
    }
}
