package com.fcproject.application.ports.inbound.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.CurrencySummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SummarizeInPort {
    List<CurrencySummary> summarize(UUID userId, LocalDate from, LocalDate to);
}
