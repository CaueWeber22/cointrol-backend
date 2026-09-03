package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.AccountBalance;

import java.util.UUID;

public interface GetAccountBalanceInPort {
    AccountBalance getBalance(UUID userId, UUID accountId);
}
