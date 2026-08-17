package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.AccountFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateAccount;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateCategory;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateEntry;
import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.Transfer;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateAccount;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateCategory;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.AccountBalance;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.CurrencySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.MonthlySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceInPort {
    Account createAccount(CreateAccount command);

    List<Account> listAccounts(AccountFilter filter);

    Account getAccount(UUID userId, UUID accountId);

    Account updateAccount(UpdateAccount command);

    void archiveAccount(UUID userId, UUID accountId);

    Category createCategory(CreateCategory command);

    List<Category> listCategories(UUID userId, CategoryKind kind, ResourceStatus status);

    Category updateCategory(UpdateCategory command);

    void archiveCategory(UUID userId, UUID categoryId);

    FinancialEntry createEntry(CreateEntry command);

    FinancialEntry getEntry(UUID userId, UUID entryId);

    PageResult<FinancialEntry> listEntries(EntryFilter filter);

    FinancialEntry updateEntry(UpdateEntry command);

    FinancialEntry cancelEntry(UUID userId, UUID entryId);

    AccountBalance getBalance(UUID userId, UUID accountId);

    TransferResult transfer(Transfer command);

    List<CurrencySummary> summarize(UUID userId, LocalDate from, LocalDate to);

    List<CategorySummary> summarizeByCategory(UUID userId, LocalDate from, LocalDate to);

    List<MonthlySummary> summarizeTimeline(UUID userId, LocalDate from, LocalDate to);
}
