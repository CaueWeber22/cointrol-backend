package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.SetDefaultAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;

public class SetDefaultAccountUsecase implements SetDefaultAccountInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public SetDefaultAccountUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public Account setDefaultAccount(UUID userId, UUID accountId) {
        Account current = requireAccount(finance, userId, accountId);
        if (current.status() == ResourceStatus.ARCHIVED) {
            throw conflict("ACCOUNT_ARCHIVED", "Archived accounts cannot be set as default");
        }
        if (current.defaultAccount()) {
            return current;
        }
        return finance.saveDefaultAccount(new Account(
                current.id(), current.userId(), current.name(), current.type(), current.currency(),
                current.status(), true, current.version(), current.createdAt(), clock.instant()
        ));
    }
}
