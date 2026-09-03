package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.AccountFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;

import java.util.List;

public interface ListAccountsInPort {
    List<Account> listAccounts(AccountFilter filter);
}
