package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateAccount;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;

public interface UpdateAccountInPort {
    Account updateAccount(UpdateAccount command);
}
