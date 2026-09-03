package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.commands.finance.FinanceCommands.AccountFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.ports.inbound.finance.ListAccountsInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.List;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;

public class ListAccountsUsecase implements ListAccountsInPort {
    private final FinanceOutPort finance;

    public ListAccountsUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public List<Account> listAccounts(AccountFilter filter) {
        requireUser(filter.userId());
        return finance.findAccounts(filter.userId(), filter.status());
    }
}
