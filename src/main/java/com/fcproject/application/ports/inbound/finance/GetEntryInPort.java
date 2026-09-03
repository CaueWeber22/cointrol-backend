package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;

import java.util.UUID;

public interface GetEntryInPort {
    FinancialEntry getEntry(UUID userId, UUID entryId);
}
