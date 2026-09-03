package com.fcproject.application.core.usecases.finance.categories;

import com.fcproject.application.core.commands.finance.FinanceCommands.CreateCategory;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.CreateCategoryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.conflict;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.normalizeName;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;
import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.rule;

public class CreateCategoryUsecase implements CreateCategoryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public CreateCategoryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public Category createCategory(CreateCategory command) {
        requireUser(command.userId());
        String name = normalizeName(command.name(), "Category name");
        if (command.kind() == null) {
            throw rule("INVALID_CATEGORY_KIND", "Category kind is required");
        }
        if (finance.existsActiveCategoryName(command.userId(), command.kind(), name, null)) {
            throw conflict("CATEGORY_NAME_CONFLICT", "An active category with this name and kind already exists");
        }
        Instant now = clock.instant();
        return finance.saveCategory(new Category(
                UUID.randomUUID(), command.userId(), name, command.kind(), ResourceStatus.ACTIVE,
                0, now, now
        ));
    }
}
