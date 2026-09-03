package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;

public interface UpdateCategoryInPort {
    Category updateCategory(UpdateCategory command);
}
