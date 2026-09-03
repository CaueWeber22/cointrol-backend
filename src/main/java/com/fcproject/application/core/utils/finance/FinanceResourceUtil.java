package com.fcproject.application.core.utils.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.exceptions.ResourceNotFoundException;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;

public final class FinanceResourceUtil {
    private FinanceResourceUtil() {
    }

    public static Account requireAccount(FinanceOutPort finance, UUID userId, UUID accountId) {
        requireUser(userId);
        if (accountId == null) {
            throw rule("ACCOUNT_REQUIRED", "Account is required");
        }
        return finance.findAccount(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    public static Account requireWritableAccount(FinanceOutPort finance, UUID userId, UUID accountId) {
        Account account = requireAccount(finance, userId, accountId);
        if (account.status() == ResourceStatus.ARCHIVED) {
            throw conflict("ACCOUNT_ARCHIVED", "Archived accounts cannot receive entries");
        }
        return account;
    }

    public static Category requireCategory(FinanceOutPort finance, UUID userId, UUID categoryId) {
        requireUser(userId);
        if (categoryId == null) {
            throw rule("CATEGORY_REQUIRED", "Category is required");
        }
        return finance.findCategory(userId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    public static Category requireWritableCategory(FinanceOutPort finance, UUID userId, UUID categoryId) {
        Category category = requireCategory(finance, userId, categoryId);
        if (category.status() == ResourceStatus.ARCHIVED) {
            throw conflict("CATEGORY_ARCHIVED", "Archived categories cannot be used in new entries");
        }
        return category;
    }

    public static FinancialEntry requireEntry(FinanceOutPort finance, UUID userId, UUID entryId) {
        requireUser(userId);
        if (entryId == null) {
            throw rule("ENTRY_REQUIRED", "Entry is required");
        }
        return finance.findEntry(userId, entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    public static TransferResult requireTransfer(FinanceOutPort finance, UUID userId, UUID transferId) {
        requireUser(userId);
        if (transferId == null) {
            throw rule("TRANSFER_REQUIRED", "Transfer is required");
        }
        return finance.findTransfer(userId, transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
    }
}
