package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.CategorySummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SummarizeByCategoryInPort {
    List<CategorySummary> summarizeByCategory(UUID userId, LocalDate from, LocalDate to);
}
