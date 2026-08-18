package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.finance.FinancialEntryEntity;
import com.fcproject.adapters.outbound.entities.finance.TransferGroupEntity;
import com.fcproject.adapters.outbound.persistence.finance.AccountJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.CategoryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.FinancialEntryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.TransferGroupJPARepository;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferStatus;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancePersistenceAdapterTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GROUP_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 16);

    @Mock
    private AccountJPARepository accounts;
    @Mock
    private CategoryJPARepository categories;
    @Mock
    private FinancialEntryJPARepository entries;
    @Mock
    private TransferGroupJPARepository transfers;
    @Mock
    private TransactionOperations transactions;

    private FinancePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FinancePersistenceAdapter(accounts, categories, entries, transfers, transactions);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsWinningTransferAfterConcurrentIdempotencyConflict() {
        TransferGroup requested = group("same-fingerprint", TransferStatus.COMPLETED, null);
        when(transactions.execute(any(TransactionCallback.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent unique key"));
        when(transfers.findByUserIdAndIdempotencyKey(USER_ID, "transfer-key"))
                .thenReturn(Optional.of(groupEntity("same-fingerprint", TransferStatus.COMPLETED, null)));
        when(entries.findAllByUserIdAndTransferGroupId(USER_ID, GROUP_ID))
                .thenReturn(List.of(
                        entryEntity(SOURCE_ID, EntryType.TRANSFER_OUT, EntryStatus.CLEARED, null),
                        entryEntity(DESTINATION_ID, EntryType.TRANSFER_IN, EntryStatus.CLEARED, null)
                ));

        var result = adapter.saveTransfer(
                requested,
                entry(SOURCE_ID, EntryType.TRANSFER_OUT, EntryStatus.CLEARED, null),
                entry(DESTINATION_ID, EntryType.TRANSFER_IN, EntryStatus.CLEARED, null)
        );

        assertEquals(GROUP_ID, result.transfer().id());
        assertEquals("same-fingerprint", result.transfer().requestFingerprint());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsConcurrentRetryWhenPayloadFingerprintDiffers() {
        when(transactions.execute(any(TransactionCallback.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent unique key"));
        when(transfers.findByUserIdAndIdempotencyKey(USER_ID, "transfer-key"))
                .thenReturn(Optional.of(groupEntity("different-fingerprint", TransferStatus.COMPLETED, null)));
        when(entries.findAllByUserIdAndTransferGroupId(USER_ID, GROUP_ID))
                .thenReturn(List.of(
                        entryEntity(SOURCE_ID, EntryType.TRANSFER_OUT, EntryStatus.CLEARED, null),
                        entryEntity(DESTINATION_ID, EntryType.TRANSFER_IN, EntryStatus.CLEARED, null)
                ));

        assertThrows(BusinessConflictException.class, () -> adapter.saveTransfer(
                group("requested-fingerprint", TransferStatus.COMPLETED, null),
                entry(SOURCE_ID, EntryType.TRANSFER_OUT, EntryStatus.CLEARED, null),
                entry(DESTINATION_ID, EntryType.TRANSFER_IN, EntryStatus.CLEARED, null)
        ));
    }

    @Test
    @SuppressWarnings("unchecked")
    void persistsGroupAndBothCanceledLegsInsideOneTransaction() {
        Instant canceledAt = NOW.plusSeconds(60);
        TransferGroup canceledGroup = group("fingerprint", TransferStatus.CANCELED, canceledAt);
        FinancialEntry debit = entry(SOURCE_ID, EntryType.TRANSFER_OUT, EntryStatus.CANCELED, canceledAt);
        FinancialEntry credit = entry(DESTINATION_ID, EntryType.TRANSFER_IN, EntryStatus.CANCELED, canceledAt);
        when(transactions.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        when(transfers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = adapter.cancelTransfer(canceledGroup, debit, credit);

        assertEquals(TransferStatus.CANCELED, result.transfer().status());
        assertEquals(EntryStatus.CANCELED, result.debit().status());
        assertEquals(EntryStatus.CANCELED, result.credit().status());
        verify(transfers).save(any());
        verify(entries, org.mockito.Mockito.times(2)).save(any());
    }

    private TransferGroup group(String fingerprint, TransferStatus status, Instant canceledAt) {
        return new TransferGroup(
                GROUP_ID, USER_ID, "transfer-key", fingerprint, status,
                canceledAt == null ? null : "conta incorreta", canceledAt, 0, NOW,
                canceledAt == null ? NOW : canceledAt
        );
    }

    private TransferGroupEntity groupEntity(String fingerprint, TransferStatus status, Instant canceledAt) {
        return new TransferGroupEntity(
                GROUP_ID, USER_ID, "transfer-key", fingerprint, status,
                canceledAt == null ? null : "conta incorreta", canceledAt, 0, NOW,
                canceledAt == null ? NOW : canceledAt
        );
    }

    private FinancialEntry entry(
            UUID accountId,
            EntryType type,
            EntryStatus status,
            Instant canceledAt
    ) {
        return new FinancialEntry(
                UUID.randomUUID(), USER_ID, accountId, null, GROUP_ID, type,
                new BigDecimal("100.0000"), status, DATE, "Reserva", null, null,
                0, canceledAt, NOW, canceledAt == null ? NOW : canceledAt
        );
    }

    private FinancialEntryEntity entryEntity(
            UUID accountId,
            EntryType type,
            EntryStatus status,
            Instant canceledAt
    ) {
        return new FinancialEntryEntity(
                UUID.randomUUID(), USER_ID, accountId, null, GROUP_ID, type,
                new BigDecimal("100.0000"), status, DATE, "Reserva", null, null,
                0, canceledAt, NOW, canceledAt == null ? NOW : canceledAt
        );
    }
}
