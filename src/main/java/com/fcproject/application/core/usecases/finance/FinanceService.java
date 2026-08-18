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
import com.fcproject.application.core.domain.finance.FinanceModels.AccountBalance;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.CurrencySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.MonthlySummary;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferStatus;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import com.fcproject.application.core.exceptions.BusinessRuleException;
import com.fcproject.application.core.exceptions.ResourceNotFoundException;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FinanceService implements FinanceInPort {
    private static final BigDecimal ZERO = new BigDecimal("0.0000");
    private static final int MAX_PAGE_SIZE = 100;

    private final FinanceOutPort finance;
    private final Clock clock;

    public FinanceService(FinanceOutPort finance, Clock clock) {
        this.finance = Objects.requireNonNull(finance);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Account createAccount(CreateAccount command) {
        requireUser(command.userId());
        String name = normalizeName(command.name(), "Account name");
        String currency = normalizeCurrency(command.currency());
        if (command.type() == null) {
            throw rule("INVALID_ACCOUNT_TYPE", "Account type is required");
        }
        if (finance.existsActiveAccountName(command.userId(), name, null)) {
            throw conflict("ACCOUNT_NAME_CONFLICT", "An active account with this name already exists");
        }
        Instant now = clock.instant();
        Account account = new Account(
                UUID.randomUUID(), command.userId(), name, command.type(), currency,
                ResourceStatus.ACTIVE, 0, now, now
        );
        if (command.openingBalance() == null) {
            return finance.saveAccount(account);
        }
        BigDecimal amount = validateAmount(command.openingBalance());
        FinancialEntry openingEntry = new FinancialEntry(
                UUID.randomUUID(), command.userId(), account.id(), null, null,
                EntryType.OPENING_BALANCE, amount, EntryStatus.CLEARED,
                LocalDate.now(clock), "Opening balance", null, null,
                0, null, now, now
        );
        return finance.saveAccountWithOpeningBalance(account, openingEntry);
    }

    @Override
    public List<Account> listAccounts(AccountFilter filter) {
        requireUser(filter.userId());
        return finance.findAccounts(filter.userId(), filter.status());
    }

    @Override
    public Account getAccount(UUID userId, UUID accountId) {
        return requireAccount(userId, accountId);
    }

    @Override
    public Account updateAccount(UpdateAccount command) {
        Account current = requireAccount(command.userId(), command.accountId());
        String name = normalizeName(command.name(), "Account name");
        if (finance.existsActiveAccountName(command.userId(), name, current.id())) {
            throw conflict("ACCOUNT_NAME_CONFLICT", "An active account with this name already exists");
        }
        return finance.saveAccount(new Account(
                current.id(), current.userId(), name, current.type(), current.currency(),
                current.status(), current.version(), current.createdAt(), clock.instant()
        ));
    }

    @Override
    public void archiveAccount(UUID userId, UUID accountId) {
        Account current = requireAccount(userId, accountId);
        if (current.status() == ResourceStatus.ARCHIVED) {
            return;
        }
        finance.saveAccount(new Account(
                current.id(), current.userId(), current.name(), current.type(), current.currency(),
                ResourceStatus.ARCHIVED, current.version(), current.createdAt(), clock.instant()
        ));
    }

    @Override
    public Category createCategory(CreateCategory command) {
        requireUser(command.userId());
        String name = normalizeName(command.name(), "Category name");
        if (command.kind() == null) {
            throw rule("INVALID_CATEGORY_KIND", "Category kind is required");
        }
        if (finance.existsActiveCategoryName(command.userId(), command.kind(), name, null)) {
            throw conflict("CATEGORY_NAME_CONFLICT", "An active category with this name and kind already exists");
        }
        Instant now = clock.instant();
        return finance.saveCategory(new Category(
                UUID.randomUUID(), command.userId(), name, command.kind(), ResourceStatus.ACTIVE,
                0, now, now
        ));
    }

    @Override
    public List<Category> listCategories(UUID userId, CategoryKind kind, ResourceStatus status) {
        requireUser(userId);
        return finance.findCategories(userId, kind, status);
    }

    @Override
    public Category updateCategory(UpdateCategory command) {
        Category current = requireCategory(command.userId(), command.categoryId());
        String name = normalizeName(command.name(), "Category name");
        if (finance.existsActiveCategoryName(command.userId(), current.kind(), name, current.id())) {
            throw conflict("CATEGORY_NAME_CONFLICT", "An active category with this name and kind already exists");
        }
        return finance.saveCategory(new Category(
                current.id(), current.userId(), name, current.kind(), current.status(),
                current.version(), current.createdAt(), clock.instant()
        ));
    }

    @Override
    public void archiveCategory(UUID userId, UUID categoryId) {
        Category current = requireCategory(userId, categoryId);
        if (current.status() == ResourceStatus.ARCHIVED) {
            return;
        }
        finance.saveCategory(new Category(
                current.id(), current.userId(), current.name(), current.kind(), ResourceStatus.ARCHIVED,
                current.version(), current.createdAt(), clock.instant()
        ));
    }

    @Override
    public FinancialEntry createEntry(CreateEntry command) {
        requireUser(command.userId());
        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        validatePublicEntryType(command.type());
        BigDecimal amount = validateAmount(command.amount());
        EntryStatus status = validateWritableStatus(command.status());
        LocalDate effectiveDate = requireDate(command.effectiveDate());
        String description = normalizeDescription(command.description());
        String fingerprint = fingerprint(
                command.accountId(), command.categoryId(), command.type(), amount,
                status, effectiveDate, description
        );

        var existing = finance.findEntryByIdempotencyKey(command.userId(), idempotencyKey);
        if (existing.isPresent()) {
            if (fingerprint.equals(existing.get().requestFingerprint())) {
                return existing.get();
            }
            throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was already used with another payload");
        }

        Account account = requireWritableAccount(command.userId(), command.accountId());
        Category category = requireWritableCategory(command.userId(), command.categoryId());
        validateCategoryCompatibility(command.type(), category);
        Instant now = clock.instant();
        return finance.saveEntry(new FinancialEntry(
                UUID.randomUUID(), command.userId(), account.id(), category.id(), null,
                command.type(), amount, status, effectiveDate, description,
                idempotencyKey, fingerprint, 0, null, now, now
        ));
    }

    @Override
    public FinancialEntry getEntry(UUID userId, UUID entryId) {
        return requireEntry(userId, entryId);
    }

    @Override
    public PageResult<FinancialEntry> listEntries(EntryFilter filter) {
        requireUser(filter.userId());
        if (filter.page() < 0 || filter.size() < 1 || filter.size() > MAX_PAGE_SIZE) {
            throw rule("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw rule("INVALID_DATE_RANGE", "Initial date must not be after final date");
        }
        return finance.findEntries(filter);
    }

    @Override
    public FinancialEntry updateEntry(UpdateEntry command) {
        FinancialEntry current = requireEntry(command.userId(), command.entryId());
        if (isTransfer(current.type())) {
            throw conflict("TRANSFER_ENTRY_IMMUTABLE", "Transfer entries must be changed through the transfer operation");
        }
        if (current.status() == EntryStatus.CANCELED) {
            throw conflict("ENTRY_CANCELED", "A canceled entry cannot be changed");
        }

        UUID accountId = command.accountId() == null ? current.accountId() : command.accountId();
        UUID categoryId = command.categoryId() == null ? current.categoryId() : command.categoryId();
        BigDecimal amount = command.amount() == null ? current.amount() : validateAmount(command.amount());
        EntryStatus status = command.status() == null ? current.status() : validateWritableStatus(command.status());
        LocalDate effectiveDate = command.effectiveDate() == null ? current.effectiveDate() : command.effectiveDate();
        String description = command.description() == null
                ? current.description()
                : normalizeDescription(command.description());

        Account currentAccount = requireAccount(command.userId(), current.accountId());
        Account targetAccount = requireWritableAccount(command.userId(), accountId);
        if (!currentAccount.currency().equals(targetAccount.currency())) {
            throw rule("CURRENCY_MISMATCH", "A transaction cannot be moved to an account in another currency");
        }
        Category category = requireWritableCategory(command.userId(), categoryId);
        validateCategoryCompatibility(current.type(), category);
        return finance.saveEntry(new FinancialEntry(
                current.id(), current.userId(), accountId, categoryId, null, current.type(),
                amount, status, effectiveDate, description, current.idempotencyKey(),
                current.requestFingerprint(), current.version(), null, current.createdAt(), clock.instant()
        ));
    }

    @Override
    public FinancialEntry cancelEntry(UUID userId, UUID entryId) {
        FinancialEntry current = requireEntry(userId, entryId);
        if (isTransfer(current.type())) {
            throw conflict("TRANSFER_ENTRY_IMMUTABLE", "Transfer entries must be canceled through the transfer operation");
        }
        if (current.status() == EntryStatus.CANCELED) {
            return current;
        }
        Instant now = clock.instant();
        return finance.saveEntry(new FinancialEntry(
                current.id(), current.userId(), current.accountId(), current.categoryId(), null,
                current.type(), current.amount(), EntryStatus.CANCELED, current.effectiveDate(),
                current.description(), current.idempotencyKey(), current.requestFingerprint(),
                current.version(), now, current.createdAt(), now
        ));
    }

    @Override
    public AccountBalance getBalance(UUID userId, UUID accountId) {
        Account account = requireAccount(userId, accountId);
        BigDecimal cleared = ZERO;
        BigDecimal pending = ZERO;
        for (FinancialEntry entry : finance.findAccountEntries(userId, accountId)) {
            if (entry.status() == EntryStatus.CANCELED) {
                continue;
            }
            BigDecimal signed = signed(entry.type(), entry.amount());
            if (entry.status() == EntryStatus.CLEARED) {
                cleared = cleared.add(signed);
            } else if (entry.status() == EntryStatus.PENDING) {
                pending = pending.add(signed);
            }
        }
        return new AccountBalance(account.id(), account.currency(), cleared, pending, cleared.add(pending));
    }

    @Override
    public TransferResult transfer(Transfer command) {
        requireUser(command.userId());
        if (Objects.equals(command.sourceAccountId(), command.destinationAccountId())) {
            throw rule("SAME_TRANSFER_ACCOUNT", "Source and destination accounts must be different");
        }
        BigDecimal amount = validateAmount(command.amount());
        LocalDate effectiveDate = requireDate(command.effectiveDate());
        String description = normalizeDescription(command.description());
        String key = normalizeIdempotencyKey(command.idempotencyKey());
        String requestFingerprint = fingerprint(
                command.sourceAccountId(), command.destinationAccountId(), amount, effectiveDate, description
        );

        var existing = finance.findTransferByIdempotencyKey(command.userId(), key);
        if (existing.isPresent()) {
            if (requestFingerprint.equals(existing.get().transfer().requestFingerprint())) {
                return existing.get();
            }
            throw conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was already used with another payload");
        }

        Account source = requireWritableAccount(command.userId(), command.sourceAccountId());
        Account destination = requireWritableAccount(command.userId(), command.destinationAccountId());
        if (!source.currency().equals(destination.currency())) {
            throw rule("CURRENCY_MISMATCH", "Transfers between different currencies are not supported");
        }

        Instant now = clock.instant();
        UUID groupId = UUID.randomUUID();
        TransferGroup group = new TransferGroup(
                groupId, command.userId(), key, requestFingerprint, TransferStatus.COMPLETED,
                null, null, 0, now, now
        );
        FinancialEntry debit = transferEntry(
                command.userId(), source.id(), groupId, EntryType.TRANSFER_OUT,
                amount, effectiveDate, description, now
        );
        FinancialEntry credit = transferEntry(
                command.userId(), destination.id(), groupId, EntryType.TRANSFER_IN,
                amount, effectiveDate, description, now
        );
        return finance.saveTransfer(group, debit, credit);
    }

    @Override
    public TransferResult getTransfer(UUID userId, UUID transferId) {
        return requireTransfer(userId, transferId);
    }

    @Override
    public TransferResult cancelTransfer(UUID userId, UUID transferId, String reason) {
        TransferResult current = requireTransfer(userId, transferId);
        if (current.transfer().status() == TransferStatus.CANCELED) {
            return current;
        }

        String normalizedReason = normalizeCancelReason(reason);
        Instant now = clock.instant();
        TransferGroup canceledGroup = new TransferGroup(
                current.transfer().id(), current.transfer().userId(), current.transfer().idempotencyKey(),
                current.transfer().requestFingerprint(), TransferStatus.CANCELED, normalizedReason, now,
                current.transfer().version(), current.transfer().createdAt(), now
        );
        FinancialEntry canceledDebit = cancelTransferLeg(current.debit(), now);
        FinancialEntry canceledCredit = cancelTransferLeg(current.credit(), now);
        return finance.cancelTransfer(canceledGroup, canceledDebit, canceledCredit);
    }

    @Override
    public List<CurrencySummary> summarize(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(userId, from, to);
        Map<String, Totals> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            Account account = context.accounts().get(entry.accountId());
            if (account == null) {
                continue;
            }
            totals.computeIfAbsent(account.currency(), ignored -> new Totals()).add(entry);
        }
        return totals.entrySet().stream()
                .map(item -> item.getValue().currency(item.getKey()))
                .sorted(Comparator.comparing(CurrencySummary::currency))
                .toList();
    }

    @Override
    public List<CategorySummary> summarizeByCategory(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(userId, from, to);
        Map<CategoryKey, BigDecimal> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            if (entry.categoryId() == null || isTransfer(entry.type()) || entry.type() == EntryType.OPENING_BALANCE) {
                continue;
            }
            Account account = context.accounts().get(entry.accountId());
            Category category = context.categories().get(entry.categoryId());
            if (account != null && category != null) {
                CategoryKey key = new CategoryKey(category.id(), category.name(), category.kind(), account.currency());
                totals.merge(key, entry.amount(), BigDecimal::add);
            }
        }
        return totals.entrySet().stream()
                .map(item -> new CategorySummary(
                        item.getKey().id(), item.getKey().name(), item.getKey().kind(),
                        item.getKey().currency(), item.getValue()
                ))
                .sorted(Comparator.comparing(CategorySummary::currency)
                        .thenComparing(CategorySummary::categoryName))
                .toList();
    }

    @Override
    public List<MonthlySummary> summarizeTimeline(UUID userId, LocalDate from, LocalDate to) {
        SummaryContext context = summaryContext(userId, from, to);
        Map<MonthKey, Totals> totals = new HashMap<>();
        for (FinancialEntry entry : context.entries()) {
            Account account = context.accounts().get(entry.accountId());
            if (account != null) {
                MonthKey key = new MonthKey(YearMonth.from(entry.effectiveDate()), account.currency());
                totals.computeIfAbsent(key, ignored -> new Totals()).add(entry);
            }
        }
        return totals.entrySet().stream()
                .map(item -> item.getValue().month(item.getKey().month(), item.getKey().currency()))
                .sorted(Comparator.comparing(MonthlySummary::month)
                        .thenComparing(MonthlySummary::currency))
                .toList();
    }

    private SummaryContext summaryContext(UUID userId, LocalDate from, LocalDate to) {
        requireUser(userId);
        LocalDate start = requireDate(from);
        LocalDate end = requireDate(to);
        if (start.isAfter(end)) {
            throw rule("INVALID_DATE_RANGE", "Initial date must not be after final date");
        }
        Map<UUID, Account> accounts = new HashMap<>();
        finance.findAccounts(userId, null).forEach(account -> accounts.put(account.id(), account));
        Map<UUID, Category> categories = new HashMap<>();
        finance.findCategories(userId, null, null).forEach(category -> categories.put(category.id(), category));
        return new SummaryContext(finance.findClearedEntries(userId, start, end), accounts, categories);
    }

    private Account requireAccount(UUID userId, UUID accountId) {
        requireUser(userId);
        if (accountId == null) {
            throw rule("ACCOUNT_REQUIRED", "Account is required");
        }
        return finance.findAccount(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Account requireWritableAccount(UUID userId, UUID accountId) {
        Account account = requireAccount(userId, accountId);
        if (account.status() == ResourceStatus.ARCHIVED) {
            throw conflict("ACCOUNT_ARCHIVED", "Archived accounts cannot receive entries");
        }
        return account;
    }

    private Category requireCategory(UUID userId, UUID categoryId) {
        requireUser(userId);
        if (categoryId == null) {
            throw rule("CATEGORY_REQUIRED", "Category is required");
        }
        return finance.findCategory(userId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Category requireWritableCategory(UUID userId, UUID categoryId) {
        Category category = requireCategory(userId, categoryId);
        if (category.status() == ResourceStatus.ARCHIVED) {
            throw conflict("CATEGORY_ARCHIVED", "Archived categories cannot be used in new entries");
        }
        return category;
    }

    private FinancialEntry requireEntry(UUID userId, UUID entryId) {
        requireUser(userId);
        if (entryId == null) {
            throw rule("ENTRY_REQUIRED", "Entry is required");
        }
        return finance.findEntry(userId, entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private FinancialEntry transferEntry(
            UUID userId,
            UUID accountId,
            UUID groupId,
            EntryType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            Instant now
    ) {
        return new FinancialEntry(
                UUID.randomUUID(), userId, accountId, null, groupId, type, amount,
                EntryStatus.CLEARED, date, description, null, null, 0, null, now, now
        );
    }

    private FinancialEntry cancelTransferLeg(FinancialEntry entry, Instant canceledAt) {
        return new FinancialEntry(
                entry.id(), entry.userId(), entry.accountId(), null, entry.transferGroupId(),
                entry.type(), entry.amount(), EntryStatus.CANCELED, entry.effectiveDate(), entry.description(),
                null, null, entry.version(), canceledAt, entry.createdAt(), canceledAt
        );
    }

    private TransferResult requireTransfer(UUID userId, UUID transferId) {
        requireUser(userId);
        if (transferId == null) {
            throw rule("TRANSFER_REQUIRED", "Transfer is required");
        }
        return finance.findTransfer(userId, transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
    }

    private void validateCategoryCompatibility(EntryType type, Category category) {
        boolean valid = type == EntryType.INCOME && category.kind() == CategoryKind.INCOME
                || type == EntryType.EXPENSE && category.kind() == CategoryKind.EXPENSE;
        if (!valid) {
            throw rule("CATEGORY_TYPE_MISMATCH", "Category kind is incompatible with transaction type");
        }
    }

    private void validatePublicEntryType(EntryType type) {
        if (type != EntryType.INCOME && type != EntryType.EXPENSE) {
            throw rule("INVALID_ENTRY_TYPE", "Only INCOME and EXPENSE can be created directly");
        }
    }

    private EntryStatus validateWritableStatus(EntryStatus status) {
        if (status == null) {
            throw rule("INVALID_ENTRY_STATUS", "Transaction status is required");
        }
        if (status == EntryStatus.CANCELED) {
            throw rule("INVALID_ENTRY_STATUS", "Use the cancellation operation to cancel a transaction");
        }
        return status;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 4) {
            throw rule("INVALID_MONEY_AMOUNT", "Amount must be positive and have at most four decimal places");
        }
        if (amount.precision() - amount.scale() > 15) {
            throw rule("INVALID_MONEY_AMOUNT", "Amount exceeds the supported limit");
        }
        return amount.setScale(4);
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw rule("INVALID_CURRENCY", "Currency is required");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw rule("INVALID_CURRENCY", "Currency must be a valid ISO 4217 code");
        }
        return normalized;
    }

    private String normalizeName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw rule("INVALID_NAME", field + " is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            throw rule("INVALID_NAME", field + " must have at most 100 characters");
        }
        return normalized;
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 255) {
            throw rule("INVALID_DESCRIPTION", "Description must have at most 255 characters");
        }
        return normalized;
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw rule("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }
        String normalized = value.strip();
        if (normalized.length() > 100) {
            throw rule("INVALID_IDEMPOTENCY_KEY", "Idempotency key must have at most 100 characters");
        }
        return normalized;
    }

    private String normalizeCancelReason(String value) {
        if (value == null || value.isBlank()) {
            throw rule("CANCEL_REASON_REQUIRED", "Cancellation reason is required");
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() > 255) {
            throw rule("INVALID_CANCEL_REASON", "Cancellation reason must have at most 255 characters");
        }
        return normalized;
    }

    private LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw rule("DATE_REQUIRED", "Effective date is required");
        }
        return value;
    }

    private void requireUser(UUID userId) {
        if (userId == null) {
            throw rule("USER_REQUIRED", "Authenticated user is required");
        }
    }

    private BigDecimal signed(EntryType type, BigDecimal amount) {
        return switch (type) {
            case INCOME, OPENING_BALANCE, TRANSFER_IN -> amount;
            case EXPENSE, TRANSFER_OUT -> amount.negate();
        };
    }

    private boolean isTransfer(EntryType type) {
        return type == EntryType.TRANSFER_IN || type == EntryType.TRANSFER_OUT;
    }

    private String fingerprint(Object... values) {
        String canonical = java.util.Arrays.stream(values)
                .map(value -> value instanceof BigDecimal decimal
                        ? decimal.stripTrailingZeros().toPlainString()
                        : String.valueOf(value))
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private BusinessRuleException rule(String code, String message) {
        return new BusinessRuleException(code, message);
    }

    private BusinessConflictException conflict(String code, String message) {
        return new BusinessConflictException(code, message);
    }

    private record SummaryContext(
            List<FinancialEntry> entries,
            Map<UUID, Account> accounts,
            Map<UUID, Category> categories
    ) {
    }

    private record CategoryKey(UUID id, String name, CategoryKind kind, String currency) {
    }

    private record MonthKey(YearMonth month, String currency) {
    }

    private static final class Totals {
        private BigDecimal income = ZERO;
        private BigDecimal expenses = ZERO;

        void add(FinancialEntry entry) {
            switch (entry.type()) {
                case INCOME -> income = income.add(entry.amount());
                case EXPENSE -> expenses = expenses.add(entry.amount());
                case OPENING_BALANCE, TRANSFER_IN, TRANSFER_OUT -> {
                    // Movimentações patrimoniais não são receita nem despesa do período.
                }
            }
        }

        CurrencySummary currency(String currency) {
            return new CurrencySummary(currency, income, expenses, income.subtract(expenses));
        }

        MonthlySummary month(YearMonth month, String currency) {
            return new MonthlySummary(month, currency, income, expenses, income.subtract(expenses));
        }
    }
}
