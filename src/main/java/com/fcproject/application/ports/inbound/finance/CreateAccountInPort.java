package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateAccount;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;

public interface CreateAccountInPort {
    Account createAccount(CreateAccount command);
}
