package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.AccountBalance;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.inbound.finance.GetAccountBalanceInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.ZERO;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.signed;

public class GetAccountBalanceUsecase implements GetAccountBalanceInPort {
    private final FinanceOutPort finance;

    public GetAccountBalanceUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public AccountBalance getBalance(UUID userId, UUID accountId) {
        Account account = requireAccount(finance, userId, accountId);
        BigDecimal cleared = ZERO;
        BigDecimal pending = ZERO;
        for (FinancialEntry entry : finance.findAccountEntries(userId, accountId)) {
            if (entry.status() == EntryStatus.CANCELED) {
                continue;
            }
            BigDecimal signed = signed(entry.type(), entry.amount());
            if (entry.status() == EntryStatus.CLEARED) {
                cleared = cleared.add(signed);
            } else if (entry.status() == EntryStatus.PENDING) {
                pending = pending.add(signed);
            }
        }
        return new AccountBalance(account.id(), account.currency(), cleared, pending, cleared.add(pending));
    }
}
