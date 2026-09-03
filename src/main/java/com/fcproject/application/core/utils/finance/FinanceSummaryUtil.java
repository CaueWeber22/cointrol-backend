package com.fcproject.application.core.utils.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireDate;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;

public final class FinanceSummaryUtil {
    private FinanceSummaryUtil() {
    }

    public static SummaryContext summaryContext(FinanceOutPort finance, UUID userId, LocalDate from, LocalDate to) {
        requireUser(userId);
        LocalDate start = requireDate(from);
        LocalDate end = requireDate(to);
        if (start.isAfter(end)) {
            throw rule("INVALID_DATE_RANGE", "Initial date must not be after final date");
        }
        Map<UUID, Account> accounts = new HashMap<>();
        finance.findAccounts(userId, null).forEach(account -> accounts.put(account.id(), account));
        Map<UUID, Category> categories = new HashMap<>();
        finance.findCategories(userId, null, null).forEach(category -> categories.put(category.id(), category));
        return new SummaryContext(finance.findClearedEntries(userId, start, end), accounts, categories);
    }

    public record SummaryContext(
            List<FinancialEntry> entries,
            Map<UUID, Account> accounts,
            Map<UUID, Category> categories
    ) {
    }
}
