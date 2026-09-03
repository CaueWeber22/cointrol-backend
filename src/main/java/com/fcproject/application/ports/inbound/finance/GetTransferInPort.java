package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

import java.util.UUID;

public interface GetTransferInPort {
    TransferResult getTransfer(UUID userId, UUID transferId);
}
