package com.fcproject.application.core.utils.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import com.fcproject.application.core.exceptions.BusinessRuleException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

public final class FinanceValidationUtil {
    public static final BigDecimal ZERO = new BigDecimal("0.0000");
    public static final int MAX_PAGE_SIZE = 100;

    private FinanceValidationUtil() {
    }

    public static void validateCategoryCompatibility(EntryType type, Category category) {
        boolean valid = type == EntryType.INCOME && category.kind() == CategoryKind.INCOME
                || type == EntryType.EXPENSE && category.kind() == CategoryKind.EXPENSE;
        if (!valid) {
            throw rule("CATEGORY_TYPE_MISMATCH", "Category kind is incompatible with transaction type");
        }
    }

    public static void validatePublicEntryType(EntryType type) {
        if (type != EntryType.INCOME && type != EntryType.EXPENSE) {
            throw rule("INVALID_ENTRY_TYPE", "Only INCOME and EXPENSE can be created directly");
        }
    }

    public static EntryStatus validateWritableStatus(EntryStatus status) {
        if (status == null) {
            throw rule("INVALID_ENTRY_STATUS", "Transaction status is required");
        }
        if (status == EntryStatus.CANCELED) {
            throw rule("INVALID_ENTRY_STATUS", "Use the cancellation operation to cancel a transaction");
        }
        return status;
    }

    public static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 4) {
            throw rule("INVALID_MONEY_AMOUNT", "Amount must be positive and have at most four decimal places");
        }
        if (amount.precision() - amount.scale() > 15) {
            throw rule("INVALID_MONEY_AMOUNT", "Amount exceeds the supported limit");
        }
        return amount.setScale(4);
    }

    public static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw rule("INVALID_CURRENCY", "Currency is required");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw rule("INVALID_CURRENCY", "Currency must be a valid ISO 4217 code");
        }
        return normalized;
    }

    public static String normalizeName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw rule("INVALID_NAME", field + " is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            throw rule("INVALID_NAME", field + " must have at most 100 characters");
        }
        return normalized;
    }

    public static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 255) {
            throw rule("INVALID_DESCRIPTION", "Description must have at most 255 characters");
        }
        return normalized;
    }

    public static String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw rule("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }
        String normalized = value.strip();
        if (normalized.length() > 100) {
            throw rule("INVALID_IDEMPOTENCY_KEY", "Idempotency key must have at most 100 characters");
        }
        return normalized;
    }

    public static String normalizeCancelReason(String value) {
        if (value == null || value.isBlank()) {
            throw rule("CANCEL_REASON_REQUIRED", "Cancellation reason is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 255) {
            throw rule("INVALID_CANCEL_REASON", "Cancellation reason must have at most 255 characters");
        }
        return normalized;
    }

    public static LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw rule("DATE_REQUIRED", "Effective date is required");
        }
        return value;
    }

    public static void requireUser(UUID userId) {
        if (userId == null) {
            throw rule("USER_REQUIRED", "Authenticated user is required");
        }
    }

    public static BigDecimal signed(EntryType type, BigDecimal amount) {
        return switch (type) {
            case INCOME, OPENING_BALANCE, TRANSFER_IN -> amount;
            case EXPENSE, TRANSFER_OUT -> amount.negate();
        };
    }

    public static boolean isTransfer(EntryType type) {
        return type == EntryType.TRANSFER_IN || type == EntryType.TRANSFER_OUT;
    }

    public static String fingerprint(Object... values) {
        String canonical = java.util.Arrays.stream(values)
                .map(value -> value instanceof BigDecimal decimal
                        ? decimal.stripTrailingZeros().toPlainString()
                        : String.valueOf(value))
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static BusinessRuleException rule(String code, String message) {
        return new BusinessRuleException(code, message);
    }

    public static BusinessConflictException conflict(String code, String message) {
        return new BusinessConflictException(code, message);
    }
}
