package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.exceptions.ResourceNotFoundException;
import com.fcproject.application.ports.inbound.finance.GetDefaultAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;

public class GetDefaultAccountUsecase implements GetDefaultAccountInPort {
    private final FinanceOutPort finance;

    public GetDefaultAccountUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public Account getDefaultAccount(UUID userId) {
        requireUser(userId);
        return finance.findDefaultAccount(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }
}
