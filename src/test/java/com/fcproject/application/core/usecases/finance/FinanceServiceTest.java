package com.fcproject.application.core.usecases.finance;

import com.fcproject.application.core.commands.finance.FinanceCommands.AccountFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateAccount;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateCategory;
import com.fcproject.application.core.commands.finance.FinanceCommands.CreateEntry;
import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.commands.finance.FinanceCommands.Transfer;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateAccount;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateCategory;
import com.fcproject.application.core.commands.finance.FinanceCommands.UpdateEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import com.fcproject.application.core.exceptions.BusinessRuleException;
import com.fcproject.application.core.exceptions.ResourceNotFoundException;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fcproject.application.core.domain.finance.FinanceModels.AccountType.CHECKING;
import static com.fcproject.application.core.domain.finance.FinanceModels.AccountType.SAVINGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ENTRY_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 16);

    @Mock
    private FinanceOutPort finance;

    private FinanceService service;

    @BeforeEach
    void setUp() {
        service = new FinanceService(finance, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsNormalizedAccount() {
        when(finance.existsActiveAccountName(USER_ID, "Conta Principal", null)).thenReturn(false);
        when(finance.saveAccount(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.createAccount(new CreateAccount(
                USER_ID, "  Conta   Principal ", CHECKING, "brl", null
        ));

        assertEquals("Conta Principal", created.name());
        assertEquals("BRL", created.currency());
        assertEquals(ResourceStatus.ACTIVE, created.status());
        assertEquals(USER_ID, created.userId());
    }

    @Test
    void persistsOpeningBalanceAsAnAtomicLedgerEntry() {
        when(finance.existsActiveAccountName(USER_ID, "Carteira", null)).thenReturn(false);
        when(finance.saveAccountWithOpeningBalance(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.createAccount(new CreateAccount(
                USER_ID, "Carteira", CHECKING, "BRL", new BigDecimal("250.00")
        ));

        assertEquals("Carteira", created.name());
        verify(finance).saveAccountWithOpeningBalance(any(), argThat(entry ->
                entry.type() == EntryType.OPENING_BALANCE
                        && entry.status() == EntryStatus.CLEARED
                        && entry.amount().equals(new BigDecimal("250.0000"))
                        && entry.accountId().equals(created.id())
        ));
    }

    @Test
    void rejectsDuplicateAccountNameAndInvalidCurrency() {
        when(finance.existsActiveAccountName(USER_ID, "Principal", null)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () ->
                service.createAccount(new CreateAccount(USER_ID, "Principal", CHECKING, "BRL", null)));
        assertThrows(BusinessRuleException.class, () ->
                service.createAccount(new CreateAccount(USER_ID, "Outra", CHECKING, "XYZ", null)));
    }

    @Test
    void updatesAndArchivesOnlyOwnedAccount() {
        Account current = account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE);
        when(finance.findAccount(USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(current));
        when(finance.existsActiveAccountName(USER_ID, "Renomeada", ACCOUNT_ID)).thenReturn(false);
        when(finance.saveAccount(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Renomeada", service.updateAccount(new UpdateAccount(USER_ID, ACCOUNT_ID, "Renomeada")).name());
        service.archiveAccount(USER_ID, ACCOUNT_ID);

        verify(finance, org.mockito.Mockito.times(2)).findAccount(USER_ID, ACCOUNT_ID);
        verify(finance, org.mockito.Mockito.times(2)).saveAccount(any());
        assertThrows(ResourceNotFoundException.class, () -> service.getAccount(USER_ID, SECOND_ACCOUNT_ID));
    }

    @Test
    void createsAndArchivesCategory() {
        when(finance.existsActiveCategoryName(USER_ID, CategoryKind.EXPENSE, "Moradia", null)).thenReturn(false);
        when(finance.saveCategory(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Category category = service.createCategory(new CreateCategory(USER_ID, " Moradia ", CategoryKind.EXPENSE));
        when(finance.findCategory(USER_ID, category.id())).thenReturn(Optional.of(category));
        when(finance.existsActiveCategoryName(USER_ID, CategoryKind.EXPENSE, "Casa", category.id()))
                .thenReturn(false);

        assertEquals("Casa", service.updateCategory(new UpdateCategory(USER_ID, category.id(), "Casa")).name());
        service.archiveCategory(USER_ID, category.id());

        verify(finance, org.mockito.Mockito.times(3)).saveCategory(any());
    }

    @Test
    void createsTransactionAndReturnsSameResultForIdempotentRetry() {
        Account account = account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE);
        Category category = category(CATEGORY_ID, "Salário", CategoryKind.INCOME, ResourceStatus.ACTIVE);
        when(finance.findEntryByIdempotencyKey(USER_ID, "entry-1"))
                .thenReturn(Optional.empty());
        when(finance.findAccount(USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(finance.findCategory(USER_ID, CATEGORY_ID)).thenReturn(Optional.of(category));
        when(finance.saveEntry(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEntry command = createEntry(EntryType.INCOME, EntryStatus.CLEARED, new BigDecimal("1000.00"), "entry-1");
        FinancialEntry created = service.createEntry(command);
        when(finance.findEntryByIdempotencyKey(USER_ID, "entry-1")).thenReturn(Optional.of(created));

        assertEquals(new BigDecimal("1000.0000"), created.amount());
        assertEquals(created, service.createEntry(command));
        verify(finance, org.mockito.Mockito.times(1)).saveEntry(any());
    }

    @Test
    void rejectsIdempotencyConflictAndCategoryMismatch() {
        FinancialEntry existing = entry(
                ENTRY_ID, ACCOUNT_ID, CATEGORY_ID, EntryType.INCOME,
                new BigDecimal("10.0000"), EntryStatus.CLEARED, "different"
        );
        when(finance.findEntryByIdempotencyKey(USER_ID, "entry-1")).thenReturn(Optional.of(existing));

        assertThrows(BusinessConflictException.class, () -> service.createEntry(
                createEntry(EntryType.INCOME, EntryStatus.CLEARED, new BigDecimal("10"), "entry-1")
        ));

        when(finance.findEntryByIdempotencyKey(USER_ID, "entry-2")).thenReturn(Optional.empty());
        when(finance.findAccount(USER_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE)));
        when(finance.findCategory(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, "Moradia", CategoryKind.EXPENSE, ResourceStatus.ACTIVE)));

        assertThrows(BusinessRuleException.class, () -> service.createEntry(
                createEntry(EntryType.INCOME, EntryStatus.CLEARED, new BigDecimal("10"), "entry-2")
        ));
    }

    @Test
    void updatesAndCancelsOrdinaryTransaction() {
        FinancialEntry current = entry(
                ENTRY_ID, ACCOUNT_ID, CATEGORY_ID, EntryType.EXPENSE,
                new BigDecimal("20.0000"), EntryStatus.PENDING, "fingerprint"
        );
        when(finance.findEntry(USER_ID, ENTRY_ID)).thenReturn(Optional.of(current));
        when(finance.findAccount(USER_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE)));
        when(finance.findCategory(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, "Mercado", CategoryKind.EXPENSE, ResourceStatus.ACTIVE)));
        when(finance.saveEntry(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialEntry updated = service.updateEntry(new UpdateEntry(
                USER_ID, ENTRY_ID, null, null, new BigDecimal("25.50"), EntryStatus.CLEARED, null, " Compras "
        ));
        when(finance.findEntry(USER_ID, ENTRY_ID)).thenReturn(Optional.of(updated));
        FinancialEntry canceled = service.cancelEntry(USER_ID, ENTRY_ID);

        assertEquals(new BigDecimal("25.5000"), updated.amount());
        assertEquals(EntryStatus.CANCELED, canceled.status());
        assertEquals(NOW, canceled.canceledAt());
    }

    @Test
    void doesNotMoveTransactionBetweenCurrencies() {
        FinancialEntry current = entry(
                ENTRY_ID, ACCOUNT_ID, CATEGORY_ID, EntryType.EXPENSE,
                new BigDecimal("20.0000"), EntryStatus.CLEARED, "fingerprint"
        );
        when(finance.findEntry(USER_ID, ENTRY_ID)).thenReturn(Optional.of(current));
        when(finance.findAccount(USER_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE)));
        when(finance.findAccount(USER_ID, SECOND_ACCOUNT_ID))
                .thenReturn(Optional.of(account(SECOND_ACCOUNT_ID, "Dollar", "USD", ResourceStatus.ACTIVE)));

        assertThrows(BusinessRuleException.class, () -> service.updateEntry(new UpdateEntry(
                USER_ID, ENTRY_ID, SECOND_ACCOUNT_ID, null, null, null, null, null
        )));
        verify(finance, never()).saveEntry(any());
    }

    @Test
    void calculatesClearedPendingAndProjectedBalances() {
        when(finance.findAccount(USER_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE)));
        when(finance.findAccountEntries(USER_ID, ACCOUNT_ID)).thenReturn(List.of(
                entry(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, EntryType.INCOME, new BigDecimal("100.0000"), EntryStatus.CLEARED, null),
                entry(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, EntryType.EXPENSE, new BigDecimal("30.0000"), EntryStatus.CLEARED, null),
                entry(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, EntryType.EXPENSE, new BigDecimal("10.0000"), EntryStatus.PENDING, null),
                entry(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, EntryType.EXPENSE, new BigDecimal("99.0000"), EntryStatus.CANCELED, null)
        ));

        var balance = service.getBalance(USER_ID, ACCOUNT_ID);

        assertEquals(new BigDecimal("70.0000"), balance.cleared());
        assertEquals(new BigDecimal("-10.0000"), balance.pending());
        assertEquals(new BigDecimal("60.0000"), balance.projected());
    }

    @Test
    void transfersAtomicallyAndSupportsIdempotentRetry() {
        Account source = account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE);
        Account destination = account(SECOND_ACCOUNT_ID, "Reserva", "BRL", ResourceStatus.ACTIVE);
        when(finance.findTransferByIdempotencyKey(USER_ID, "transfer-1")).thenReturn(Optional.empty());
        when(finance.findAccount(USER_ID, ACCOUNT_ID)).thenReturn(Optional.of(source));
        when(finance.findAccount(USER_ID, SECOND_ACCOUNT_ID)).thenReturn(Optional.of(destination));
        when(finance.saveTransfer(any(), any(), any())).thenAnswer(invocation ->
                new TransferResult(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        Transfer command = new Transfer(
                USER_ID, ACCOUNT_ID, SECOND_ACCOUNT_ID, new BigDecimal("50"), DATE, "Reserva", "transfer-1"
        );
        TransferResult result = service.transfer(command);
        when(finance.findTransferByIdempotencyKey(USER_ID, "transfer-1")).thenReturn(Optional.of(result));

        assertEquals(EntryType.TRANSFER_OUT, result.debit().type());
        assertEquals(EntryType.TRANSFER_IN, result.credit().type());
        assertEquals(result.transfer().id(), result.debit().transferGroupId());
        assertEquals(result, service.transfer(command));
        verify(finance, org.mockito.Mockito.times(1)).saveTransfer(any(), any(), any());
    }

    @Test
    void rejectsInvalidTransfers() {
        assertThrows(BusinessRuleException.class, () -> service.transfer(new Transfer(
                USER_ID, ACCOUNT_ID, ACCOUNT_ID, BigDecimal.ONE, DATE, null, "same"
        )));

        when(finance.findTransferByIdempotencyKey(USER_ID, "currency")).thenReturn(Optional.empty());
        when(finance.findAccount(USER_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE)));
        when(finance.findAccount(USER_ID, SECOND_ACCOUNT_ID))
                .thenReturn(Optional.of(account(SECOND_ACCOUNT_ID, "Dollar", "USD", ResourceStatus.ACTIVE)));

        assertThrows(BusinessRuleException.class, () -> service.transfer(new Transfer(
                USER_ID, ACCOUNT_ID, SECOND_ACCOUNT_ID, BigDecimal.ONE, DATE, null, "currency"
        )));
        verify(finance, never()).saveTransfer(any(), any(), any());
    }

    @Test
    void createsCurrencyCategoryAndMonthlySummaries() {
        Account account = account(ACCOUNT_ID, "Principal", "BRL", ResourceStatus.ACTIVE);
        Category income = category(CATEGORY_ID, "Salário", CategoryKind.INCOME, ResourceStatus.ACTIVE);
        List<FinancialEntry> entries = List.of(
                entry(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, EntryType.INCOME, new BigDecimal("100.0000"), EntryStatus.CLEARED, null),
                entry(UUID.randomUUID(), ACCOUNT_ID, null, EntryType.TRANSFER_OUT, new BigDecimal("20.0000"), EntryStatus.CLEARED, null)
        );
        when(finance.findAccounts(USER_ID, null)).thenReturn(List.of(account));
        when(finance.findCategories(USER_ID, null, null)).thenReturn(List.of(income));
        when(finance.findClearedEntries(USER_ID, DATE.minusDays(1), DATE.plusDays(1))).thenReturn(entries);

        var summary = service.summarize(USER_ID, DATE.minusDays(1), DATE.plusDays(1));
        var categories = service.summarizeByCategory(USER_ID, DATE.minusDays(1), DATE.plusDays(1));
        var timeline = service.summarizeTimeline(USER_ID, DATE.minusDays(1), DATE.plusDays(1));

        assertEquals(new BigDecimal("100.0000"), summary.getFirst().net());
        assertEquals(new BigDecimal("0.0000"), summary.getFirst().expenses());
        assertEquals(new BigDecimal("100.0000"), categories.getFirst().amount());
        assertEquals("2026-08", timeline.getFirst().month().toString());
    }

    @Test
    void validatesPaginationAndDateRanges() {
        assertThrows(BusinessRuleException.class, () -> service.listEntries(new EntryFilter(
                USER_ID, null, null, null, null, null, null, -1, 20
        )));
        assertThrows(BusinessRuleException.class, () -> service.summarize(USER_ID, DATE, DATE.minusDays(1)));

        PageResult<FinancialEntry> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        EntryFilter valid = new EntryFilter(USER_ID, null, null, null, null, null, null, 0, 20);
        when(finance.findEntries(valid)).thenReturn(expected);
        assertEquals(expected, service.listEntries(valid));
        assertTrue(service.listAccounts(new AccountFilter(USER_ID, null)).isEmpty());
    }

    private CreateEntry createEntry(EntryType type, EntryStatus status, BigDecimal amount, String key) {
        return new CreateEntry(USER_ID, ACCOUNT_ID, CATEGORY_ID, type, amount, status, DATE, "Teste", key);
    }

    private Account account(UUID id, String name, String currency, ResourceStatus status) {
        return new Account(id, USER_ID, name, id.equals(SECOND_ACCOUNT_ID) ? SAVINGS : CHECKING,
                currency, status, 0, NOW, NOW);
    }

    private Category category(UUID id, String name, CategoryKind kind, ResourceStatus status) {
        return new Category(id, USER_ID, name, kind, status, 0, NOW, NOW);
    }

    private FinancialEntry entry(
            UUID id, UUID accountId, UUID categoryId, EntryType type,
            BigDecimal amount, EntryStatus status, String fingerprint
    ) {
        return new FinancialEntry(
                id, USER_ID, accountId, categoryId, type == EntryType.TRANSFER_IN || type == EntryType.TRANSFER_OUT
                ? UUID.randomUUID() : null, type, amount, status, DATE, "Teste", "entry-1", fingerprint,
                0, status == EntryStatus.CANCELED ? NOW : null, NOW, NOW
        );
    }
}
