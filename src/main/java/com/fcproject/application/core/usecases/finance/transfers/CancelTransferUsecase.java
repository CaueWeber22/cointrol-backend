package com.fcproject.application.core.usecases.finance.transfers;

import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferStatus;
import com.fcproject.application.ports.inbound.finance.CancelTransferInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireTransfer;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeCancelReason;

public class CancelTransferUsecase implements CancelTransferInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CancelTransferUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public TransferResult cancelTransfer(UUID userId, UUID transferId, String reason) {
        TransferResult current = requireTransfer(finance, userId, transferId);
        if (current.transfer().status() == TransferStatus.CANCELED) {
            return current;
        }

        String normalizedReason = normalizeCancelReason(reason);
        Instant now = clock.instant();
        TransferGroup canceledGroup = new TransferGroup(
                current.transfer().id(), current.transfer().userId(), current.transfer().idempotencyKey(),
                current.transfer().requestFingerprint(), TransferStatus.CANCELED, normalizedReason, now,
                current.transfer().version(), current.transfer().createdAt(), now
        );
        FinancialEntry canceledDebit = cancelTransferLeg(current.debit(), now);
        FinancialEntry canceledCredit = cancelTransferLeg(current.credit(), now);
        return finance.cancelTransfer(canceledGroup, canceledDebit, canceledCredit);
    }

    private FinancialEntry cancelTransferLeg(FinancialEntry entry, Instant canceledAt) {
        return new FinancialEntry(
                entry.id(), entry.userId(), entry.accountId(), null, entry.transferGroupId(),
                entry.type(), entry.amount(), EntryStatus.CANCELED, entry.effectiveDate(), entry.description(),
                null, null, entry.version(), canceledAt, entry.createdAt(), canceledAt
        );
    }
}
