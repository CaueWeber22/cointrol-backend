package com.fcproject.application.core.usecases.finance.summaries;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.MonthlySummary;
import com.fcproject.application.core.utils.finance.FinanceSummaryUtil.SummaryContext;
import com.fcproject.application.ports.inbound.finance.SummarizeTimelineInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceSummaryUtil.summaryContext;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.ZERO;

public class SummarizeTimelineUsecase implements SummarizeTimelineInPort {
    private final FinanceOutPort finance;

    public SummarizeTimelineUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public List<MonthlySummary> summarizeTimeline(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(finance, userId, from, to);
        Map<MonthKey, Totals> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            Account account = context.accounts().get(entry.accountId());
            if (account != null) {
                MonthKey key = new MonthKey(YearMonth.from(entry.effectiveDate()), account.currency());
                totals.computeIfAbsent(key, ignored -> new Totals()).add(entry);
            }
        }
        return totals.entrySet().stream()
                .map(item -> item.getValue().month(item.getKey().month(), item.getKey().currency()))
                .sorted(Comparator.comparing(MonthlySummary::month)
                        .thenComparing(MonthlySummary::currency))
                .toList();
    }

    private record MonthKey(YearMonth month, String currency) {
    }

    private static final class Totals {
        private BigDecimal income = ZERO;
        private BigDecimal expenses = ZERO;

        void add(FinancialEntry entry) {
            switch (entry.type()) {
                case INCOME -> income = income.add(entry.amount());
                case EXPENSE -> expenses = expenses.add(entry.amount());
                case OPENING_BALANCE, TRANSFER_IN, TRANSFER_OUT -> {
                    // Equity movements are not income or expenses for the period.
                }
            }
        }

        MonthlySummary month(YearMonth month, String currency) {
            return new MonthlySummary(month, currency, income, expenses, income.subtract(expenses));
        }
    }
}
