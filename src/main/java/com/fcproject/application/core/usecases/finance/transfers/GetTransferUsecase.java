package com.fcproject.application.core.usecases.finance.transfers;

import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.ports.inbound.finance.GetTransferInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireTransfer;

public class GetTransferUsecase implements GetTransferInPort {
    private final FinanceOutPort finance;

    public GetTransferUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public TransferResult getTransfer(UUID userId, UUID transferId) {
        return requireTransfer(finance, userId, transferId);
    }
}
