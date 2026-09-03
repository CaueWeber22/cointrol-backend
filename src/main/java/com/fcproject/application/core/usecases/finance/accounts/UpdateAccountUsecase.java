package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateAccount;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.ports.inbound.finance.UpdateAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeName;

public class UpdateAccountUsecase implements UpdateAccountInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public UpdateAccountUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public Account updateAccount(UpdateAccount command) {
        Account current = requireAccount(finance, command.userId(), command.accountId());
        String name = normalizeName(command.name(), "Account name");
        if (finance.existsActiveAccountName(command.userId(), name, current.id())) {
            throw conflict("ACCOUNT_NAME_CONFLICT", "An active account with this name already exists");
        }
        return finance.saveAccount(new Account(
                current.id(), current.userId(), name, current.type(), current.currency(),
                current.status(), current.version(), current.createdAt(), clock.instant()
        ));
    }
}
