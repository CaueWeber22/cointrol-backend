package com.fcproject.application.core.usecases.finance.entries;

import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.ports.inbound.finance.CancelEntryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireEntry;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.isTransfer;

public class CancelEntryUsecase implements CancelEntryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CancelEntryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public FinancialEntry cancelEntry(UUID userId, UUID entryId) {
        FinancialEntry current = requireEntry(finance, userId, entryId);
        if (isTransfer(current.type())) {
            throw conflict("TRANSFER_ENTRY_IMMUTABLE", "Transfer entries must be canceled through the transfer operation");
        }
        if (current.status() == EntryStatus.CANCELED) {
            return current;
        }
        Instant now = clock.instant();
        return finance.saveEntry(new FinancialEntry(
                current.id(), current.userId(), current.accountId(), current.categoryId(), null,
                current.type(), current.amount(), EntryStatus.CANCELED, current.effectiveDate(),
                current.description(), current.idempotencyKey(), current.requestFingerprint(),
                current.version(), now, current.createdAt(), now
        ));
    }
}
