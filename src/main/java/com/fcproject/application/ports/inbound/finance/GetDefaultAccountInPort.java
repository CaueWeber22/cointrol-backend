package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;

import java.util.UUID;

public interface GetDefaultAccountInPort {
    Account getDefaultAccount(UUID userId);
}
