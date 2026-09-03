package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;

public interface CreateEntryInPort {
    FinancialEntry createEntry(CreateEntry command);
}
