package com.fcproject.application.core.usecases.finance.categories;

import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.ports.inbound.finance.UpdateCategoryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireCategory;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeName;

public class UpdateCategoryUsecase implements UpdateCategoryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public UpdateCategoryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public Category updateCategory(UpdateCategory command) {
        Category current = requireCategory(finance, command.userId(), command.categoryId());
        String name = normalizeName(command.name(), "Category name");
        if (finance.existsActiveCategoryName(command.userId(), current.kind(), name, current.id())) {
            throw conflict("CATEGORY_NAME_CONFLICT", "An active category with this name and kind already exists");
        }
        return finance.saveCategory(new Category(
                current.id(), current.userId(), name, current.kind(), current.status(),
                current.version(), current.createdAt(), clock.instant()
        ));
    }
}
