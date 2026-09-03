package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;

public interface ListEntriesInPort {
    PageResult<FinancialEntry> listEntries(EntryFilter filter);
}
