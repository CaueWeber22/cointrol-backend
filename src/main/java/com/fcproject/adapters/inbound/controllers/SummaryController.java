package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.CategorySummaryResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.CurrencySummaryResponse;
import com.fcproject.adapters.inbound.dto.finance.FinanceResponses.MonthlySummaryResponse;
import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
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
    private final FinanceInPort finance;
    private final CurrentUserIdProvider currentUser;

    public SummaryController(FinanceInPort finance, CurrentUserIdProvider currentUser) {
        this.finance = finance;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CurrencySummaryResponse> summary(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return finance.summarize(currentUser.get(principal), from, to)
                .stream().map(CurrencySummaryResponse::from).toList();
    }

    @GetMapping("/by-category")
    public List<CategorySummaryResponse> byCategory(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return finance.summarizeByCategory(currentUser.get(principal), from, to)
                .stream().map(CategorySummaryResponse::from).toList();
    }

    @GetMapping("/timeline")
    public List<MonthlySummaryResponse> timeline(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return finance.summarizeTimeline(currentUser.get(principal), from, to)
                .stream().map(MonthlySummaryResponse::from).toList();
    }
}
