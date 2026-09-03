package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

import java.util.UUID;

public interface CancelTransferInPort {
    TransferResult cancelTransfer(UUID userId, UUID transferId, String reason);
}
