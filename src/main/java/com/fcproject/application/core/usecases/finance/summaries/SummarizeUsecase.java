package com.fcproject.application.core.usecases.finance.summaries;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.CurrencySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.utils.finance.FinanceSummaryUtil.SummaryContext;
import com.fcproject.application.ports.inbound.finance.SummarizeInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceSummaryUtil.summaryContext;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.ZERO;

public class SummarizeUsecase implements SummarizeInPort {
    private final FinanceOutPort finance;

    public SummarizeUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public List<CurrencySummary> summarize(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(finance, userId, from, to);
        Map<String, Totals> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            Account account = context.accounts().get(entry.accountId());
            if (account == null) {
                continue;
            }
            totals.computeIfAbsent(account.currency(), ignored -> new Totals()).add(entry);
        }
        return totals.entrySet().stream()
                .map(item -> item.getValue().currency(item.getKey()))
                .sorted(Comparator.comparing(CurrencySummary::currency))
                .toList();
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

        CurrencySummary currency(String currency) {
            return new CurrencySummary(currency, income, expenses, income.subtract(expenses));
        }
    }
}
