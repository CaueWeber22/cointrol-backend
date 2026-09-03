package com.fcproject.application.core.usecases.finance.entries;

import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.inbound.finance.GetEntryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireEntry;

public class GetEntryUsecase implements GetEntryInPort {
    private final FinanceOutPort finance;

    public GetEntryUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public FinancialEntry getEntry(UUID userId, UUID entryId) {
        return requireEntry(finance, userId, entryId);
    }
}
