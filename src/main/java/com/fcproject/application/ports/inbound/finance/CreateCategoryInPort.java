package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;

public interface CreateCategoryInPort {
    Category createCategory(CreateCategory command);
}
