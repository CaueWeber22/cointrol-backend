package com.fcproject.application.core.usecases.finance.categories;

import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.ListCategoriesInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.util.List;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceValidationUtil.requireUser;

public class ListCategoriesUsecase implements ListCategoriesInPort {
    private final FinanceOutPort finance;

    public ListCategoriesUsecase(FinanceOutPort finance) {
        this.finance = finance;
    }

    @Override
    public List<Category> listCategories(UUID userId, CategoryKind kind, ResourceStatus status) {
        requireUser(userId);
        return finance.findCategories(userId, kind, status);
    }
}
