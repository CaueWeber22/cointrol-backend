package com.fcproject.application.core.usecases.finance.summaries;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.utils.finance.FinanceSummaryUtil.SummaryContext;
import com.fcproject.application.ports.inbound.finance.SummarizeByCategoryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceSummaryUtil.summaryContext;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.isTransfer;

public class SummarizeByCategoryUsecase implements SummarizeByCategoryInPort {
    private final FinanceOutPort finance;

    public SummarizeByCategoryUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public List<CategorySummary> summarizeByCategory(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(finance, userId, from, to);
        Map<CategoryKey, BigDecimal> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            if (entry.categoryId() == null || isTransfer(entry.type()) || entry.type() == EntryType.OPENING_BALANCE) {
                continue;
            }
            Account account = context.accounts().get(entry.accountId());
            Category category = context.categories().get(entry.categoryId());
            if (account != null && category != null) {
                CategoryKey key = new CategoryKey(category.id(), category.name(), category.kind(), account.currency());
                totals.merge(key, entry.amount(), BigDecimal::add);
            }
        }
        return totals.entrySet().stream()
                .map(item -> new CategorySummary(
                        item.getKey().id(), item.getKey().name(), item.getKey().kind(),
                        item.getKey().currency(), item.getValue()
                ))
                .sorted(Comparator.comparing(CategorySummary::currency)
                        .thenComparing(CategorySummary::categoryName))
                .toList();
    }

    private record CategoryKey(UUID id, String name, CategoryKind kind, String currency) {
    }
}
