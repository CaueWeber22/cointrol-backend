package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.Transfer;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;

public interface CreateTransferInPort {
    TransferResult transfer(Transfer command);
}
