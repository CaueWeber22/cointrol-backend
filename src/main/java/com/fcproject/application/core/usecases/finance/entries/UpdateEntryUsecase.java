package com.fcproject.application.core.usecases.finance.entries;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.inbound.finance.UpdateEntryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireAccount;
import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireEntry;
import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireWritableAccount;
import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireWritableCategory;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.isTransfer;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeDescription;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateAmount;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateCategoryCompatibility;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.validateWritableStatus;

public class UpdateEntryUsecase implements UpdateEntryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public UpdateEntryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public FinancialEntry updateEntry(UpdateEntry command) {
        FinancialEntry current = requireEntry(finance, command.userId(), command.entryId());
        if (isTransfer(current.type())) {
            throw conflict("TRANSFER_ENTRY_IMMUTABLE", "Transfer entries must be changed through the transfer operation");
        }
        if (current.status() == EntryStatus.CANCELED) {
            throw conflict("ENTRY_CANCELED", "A canceled entry cannot be changed");
        }

        UUID accountId = command.accountId() == null ? current.accountId() : command.accountId();
        UUID categoryId = command.categoryId() == null ? current.categoryId() : command.categoryId();
        BigDecimal amount = command.amount() == null ? current.amount() : validateAmount(command.amount());
        EntryStatus status = command.status() == null ? current.status() : validateWritableStatus(command.status());
        LocalDate effectiveDate = command.effectiveDate() == null ? current.effectiveDate() : command.effectiveDate();
        String description = command.description() == null
                ? current.description()
                : normalizeDescription(command.description());

        Account currentAccount = requireAccount(finance, command.userId(), current.accountId());
        Account targetAccount = requireWritableAccount(finance, command.userId(), accountId);
        if (!currentAccount.currency().equals(targetAccount.currency())) {
            throw rule("CURRENCY_MISMATCH", "A transaction cannot be moved to an account in another currency");
        }
        Category category = requireWritableCategory(finance, command.userId(), categoryId);
        validateCategoryCompatibility(current.type(), category);
        return finance.saveEntry(new FinancialEntry(
                current.id(), current.userId(), accountId, categoryId, null, current.type(),
                amount, status, effectiveDate, description, current.idempotencyKey(),
                current.requestFingerprint(), current.version(), null, current.createdAt(), clock.instant()
        ));
    }
}
