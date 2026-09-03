package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.ports.inbound.finance.GetAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;

public class GetAccountUsecase implements GetAccountInPort {
    private final FinanceOutPort finance;

    public GetAccountUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public Account getAccount(UUID userId, UUID accountId) {
        return requireAccount(finance, userId, accountId);
    }
}
