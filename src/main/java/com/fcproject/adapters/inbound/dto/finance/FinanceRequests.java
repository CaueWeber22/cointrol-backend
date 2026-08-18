package com.fcproject.adapters.inbound.dto.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.AccountType;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceRequests {
    private FinanceRequests() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull AccountType type,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4)
            BigDecimal openingBalance
    ) {
    }

    public record UpdateAccountRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull CategoryKind kind
    ) {
    }

    public record UpdateCategoryRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record CreateTransactionRequest(
            @NotNull UUID accountId,
            @NotNull UUID categoryId,
            @NotNull EntryType type,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4)
            BigDecimal amount,
            @NotNull EntryStatus status,
            @NotNull LocalDate effectiveDate,
            @Size(max = 255) String description
    ) {
    }

    public record UpdateTransactionRequest(
            UUID accountId,
            UUID categoryId,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4)
            BigDecimal amount,
            EntryStatus status,
            LocalDate effectiveDate,
            @Size(max = 255) String description
    ) {
    }

    public record TransferRequest(
            @NotNull UUID sourceAccountId,
            @NotNull UUID destinationAccountId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4)
            BigDecimal amount,
            @NotNull LocalDate effectiveDate,
            @Size(max = 255) String description
    ) {
    }

    public record CancelTransferRequest(
            @NotBlank @Size(max = 255) String reason
    ) {
    }
}
