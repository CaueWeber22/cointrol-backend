package com.fcproject.application.ports.inbound.finance;

public interface FinanceInPort extends
        CreateAccountInPort,
        ListAccountsInPort,
        GetAccountInPort,
        UpdateAccountInPort,
        ArchiveAccountInPort,
        GetAccountBalanceInPort,
        CreateCategoryInPort,
        ListCategoriesInPort,
        UpdateCategoryInPort,
        ArchiveCategoryInPort,
        CreateEntryInPort,
        GetEntryInPort,
        ListEntriesInPort,
        UpdateEntryInPort,
        CancelEntryInPort,
        CreateTransferInPort,
        GetTransferInPort,
        CancelTransferInPort,
        SummarizeInPort,
        SummarizeByCategoryInPort,
        SummarizeTimelineInPort {
}
