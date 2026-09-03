package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;

public interface UpdateEntryInPort {
    FinancialEntry updateEntry(UpdateEntry command);
}
