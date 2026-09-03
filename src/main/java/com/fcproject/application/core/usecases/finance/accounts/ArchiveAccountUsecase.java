package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.ArchiveAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;

public class ArchiveAccountUsecase implements ArchiveAccountInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public ArchiveAccountUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public void archiveAccount(UUID userId, UUID accountId) {
        Account current = requireAccount(finance, userId, accountId);
        if (current.status() == ResourceStatus.ARCHIVED) {
            return;
        }
        finance.saveAccount(new Account(
                current.id(), current.userId(), current.name(), current.type(), current.currency(),
                ResourceStatus.ARCHIVED, current.version(), current.createdAt(), clock.instant()
        ));
    }
}
