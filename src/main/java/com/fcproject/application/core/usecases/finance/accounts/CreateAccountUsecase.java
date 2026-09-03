package com.fcproject.application.core.usecases.finance.accounts;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateAccount;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.CreateAccountInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeCurrency;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeName;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateAmount;

public class CreateAccountUsecase implements CreateAccountInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CreateAccountUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public Account createAccount(CreateAccount command) {
        requireUser(command.userId());
        String name = normalizeName(command.name(), "Account name");
        String currency = normalizeCurrency(command.currency());
        if (command.type() == null) {
            throw rule("INVALID_ACCOUNT_TYPE", "Account type is required");
        }
        if (finance.existsActiveAccountName(command.userId(), name, null)) {
            throw conflict("ACCOUNT_NAME_CONFLICT", "An active account with this name already exists");
        }
        Instant now = clock.instant();
        Account account = new Account(
                UUID.randomUUID(), command.userId(), name, command.type(), currency,
                ResourceStatus.ACTIVE, 0, now, now
        );
        if (command.openingBalance() == null) {
            return finance.saveAccount(account);
        }
        BigDecimal amount = validateAmount(command.openingBalance());
        FinancialEntry openingEntry = new FinancialEntry(
                UUID.randomUUID(), command.userId(), account.id(), null, null,
                EntryType.OPENING_BALANCE, amount, EntryStatus.CLEARED,
                LocalDate.now(clock), "Opening balance", null, null,
                0, null, now, now
        );
        return finance.saveAccountWithOpeningBalance(account, openingEntry);
    }
}
