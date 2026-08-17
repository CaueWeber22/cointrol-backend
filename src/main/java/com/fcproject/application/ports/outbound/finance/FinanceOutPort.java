package com.fcproject.application.ports.outbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceOutPort {
    Account saveAccount(Account account);

    Account saveAccountWithOpeningBalance(Account account, FinancialEntry openingEntry);

    Optional<Account> findAccount(UUID userId, UUID accountId);

    List<Account> findAccounts(UUID userId, ResourceStatus status);

    boolean existsActiveAccountName(UUID userId, String normalizedName, UUID ignoredId);

    Category saveCategory(Category category);

    Optional<Category> findCategory(UUID userId, UUID categoryId);

    List<Category> findCategories(UUID userId, CategoryKind kind, ResourceStatus status);

    boolean existsActiveCategoryName(UUID userId, CategoryKind kind, String normalizedName, UUID ignoredId);

    FinancialEntry saveEntry(FinancialEntry entry);

    Optional<FinancialEntry> findEntry(UUID userId, UUID entryId);

    Optional<FinancialEntry> findEntryByIdempotencyKey(UUID userId, String idempotencyKey);

    PageResult<FinancialEntry> findEntries(EntryFilter filter);

    List<FinancialEntry> findAccountEntries(UUID userId, UUID accountId);

    List<FinancialEntry> findClearedEntries(UUID userId, LocalDate from, LocalDate to);

    Optional<TransferResult> findTransferByIdempotencyKey(UUID userId, String idempotencyKey);

    TransferResult saveTransfer(TransferGroup group, FinancialEntry debit, FinancialEntry credit);
}
