package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.CategorySummaryResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.CurrencySummaryResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.MonthlySummaryResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.ports.inbound.finance.SummarizeByCategoryInPort;
import com.fcproject.application.ports.inbound.finance.SummarizeInPort;
import com.fcproject.application.ports.inbound.finance.SummarizeTimelineInPort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {
    private final SummarizeInPort summarize;
    private final SummarizeByCategoryInPort summarizeByCategory;
    private final SummarizeTimelineInPort summarizeTimeline;
    private final CurrentUserIdProvider currentUser;

    public SummaryController(
            SummarizeInPort summarize,
            SummarizeByCategoryInPort summarizeByCategory,
            SummarizeTimelineInPort summarizeTimeline,
            CurrentUserIdProvider currentUser
    ) {
        this.summarize = summarize;
        this.summarizeByCategory = summarizeByCategory;
        this.summarizeTimeline = summarizeTimeline;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CurrencySummaryResponse> summary(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return summarize.summarize(currentUser.get(principal), from, to)
                .stream().map(CurrencySummaryResponse::from).toList();
    }

    @GetMapping("/by-category")
    public List<CategorySummaryResponse> byCategory(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return summarizeByCategory.summarizeByCategory(currentUser.get(principal), from, to)
                .stream().map(CategorySummaryResponse::from).toList();
    }

    @GetMapping("/timeline")
    public List<MonthlySummaryResponse> timeline(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return summarizeTimeline.summarizeTimeline(currentUser.get(principal), from, to)
                .stream().map(MonthlySummaryResponse::from).toList();
    }
}
