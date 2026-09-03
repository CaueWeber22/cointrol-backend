package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;

import java.util.List;
import java.util.UUID;

public interface ListCategoriesInPort {
    List<Category> listCategories(UUID userId, CategoryKind kind, ResourceStatus status);
}
