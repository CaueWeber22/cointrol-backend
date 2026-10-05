package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;

import java.util.UUID;

public interface SetDefaultAccountInPort {
    Account setDefaultAccount(UUID userId, UUID accountId);
}
