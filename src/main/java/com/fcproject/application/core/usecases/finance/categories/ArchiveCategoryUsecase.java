package com.fcproject.application.core.usecases.finance.categories;

import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.ports.inbound.finance.ArchiveCategoryInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.time.Clock;
import java.util.UUID;

import static com.fcproject.application.core.utils.finance.FinanceResourceUtil.requireCategory;

public class ArchiveCategoryUsecase implements ArchiveCategoryInPort {
    private final FinanceOutPort finance;
    private final Clock clock;

    public ArchiveCategoryUsecase(FinanceOutPort finance, Clock clock) {
        this.finance = finance;
        this.clock = clock;
    }

    @Override
    public void archiveCategory(UUID userId, UUID categoryId) {
        Category current = requireCategory(finance, userId, categoryId);
        if (current.status() == ResourceStatus.ARCHIVED) {
            return;
        }
        finance.saveCategory(new Category(
                current.id(), current.userId(), current.name(), current.kind(), ResourceStatus.ARCHIVED,
                current.version(), current.createdAt(), clock.instant()
        ));
    }
}
