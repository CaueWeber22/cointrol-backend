package com.fcproject.application.core.usecases.finance.entries;

import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.ports.inbound.finance.ListEntriesInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.MAX_PAGE_SIZE;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;

public class ListEntriesUsecase implements ListEntriesInPort {
    private final FinanceOutPort finance;

    public ListEntriesUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public PageResult<FinancialEntry> listEntries(EntryFilter filter) {
        requireUser(filter.userId());
        if (filter.page() < 0 || filter.size() < 1 || filter.size() > MAX_PAGE_SIZE) {
            throw rule("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw rule("INVALID_DATE_RANGE", "Initial date must not be after final date");
        }
        return finance.findEntries(filter);
    }
}
