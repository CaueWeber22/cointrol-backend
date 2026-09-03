package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;

import java.util.UUID;

public interface CancelEntryInPort {
    FinancialEntry cancelEntry(UUID userId, UUID entryId);
}
