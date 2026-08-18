package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.finance.AccountEntity;
import com.fcproject.adapters.outbound.entities.finance.CategoryEntity;
import com.fcproject.adapters.outbound.entities.finance.FinancialEntryEntity;
import com.fcproject.adapters.outbound.entities.finance.TransferGroupEntity;
import com.fcproject.adapters.outbound.persistence.finance.AccountJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.CategoryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.FinancialEntryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.TransferGroupJPARepository;
import com.fcproject.application.core.commands.finance.FinanceCommands.EntryFilter;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.Category;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.PageResult;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.exceptions.BusinessConflictException;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

public class FinancePersistenceAdapter implements FinanceOutPort {
    private final AccountJPARepository accounts;
    private final CategoryJPARepository categories;
    private final FinancialEntryJPARepository entries;
    private final TransferGroupJPARepository transfers;
    private final TransactionOperations writeTransactions;

    public FinancePersistenceAdapter(
            AccountJPARepository accounts,
            CategoryJPARepository categories,
            FinancialEntryJPARepository entries,
            TransferGroupJPARepository transfers,
            TransactionOperations writeTransactions
    ) {
        this.accounts = accounts;
        this.categories = categories;
        this.entries = entries;
        this.transfers = transfers;
        this.writeTransactions = writeTransactions;
    }

    @Override
    public Account saveAccount(Account account) {
        return toDomain(accounts.save(toEntity(account)));
    }

    @Override
    @Transactional
    public Account saveAccountWithOpeningBalance(Account account, FinancialEntry openingEntry) {
        Account persistedAccount = toDomain(accounts.save(toEntity(account)));
        entries.save(toEntity(openingEntry));
        return persistedAccount;
    }

    @Override
    public Optional<Account> findAccount(UUID userId, UUID accountId) {
        return accounts.findByIdAndUserId(accountId, userId).map(this::toDomain);
    }

    @Override
    public List<Account> findAccounts(UUID userId, ResourceStatus status) {
        return accounts.findOwned(userId, status).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveAccountName(UUID userId, String normalizedName, UUID ignoredId) {
        return accounts.existsActiveName(userId, normalizedName, ResourceStatus.ACTIVE, ignoredId);
    }

    @Override
    public Category saveCategory(Category category) {
        return toDomain(categories.save(toEntity(category)));
    }

    @Override
    public Optional<Category> findCategory(UUID userId, UUID categoryId) {
        return categories.findByIdAndUserId(categoryId, userId).map(this::toDomain);
    }

    @Override
    public List<Category> findCategories(UUID userId, CategoryKind kind, ResourceStatus status) {
        return categories.findOwned(userId, kind, status).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveCategoryName(
            UUID userId,
            CategoryKind kind,
            String normalizedName,
            UUID ignoredId
    ) {
        return categories.existsActiveName(userId, kind, normalizedName, ResourceStatus.ACTIVE, ignoredId);
    }

    @Override
    public FinancialEntry saveEntry(FinancialEntry entry) {
        return toDomain(entries.save(toEntity(entry)));
    }

    @Override
    public Optional<FinancialEntry> findEntry(UUID userId, UUID entryId) {
        return entries.findByIdAndUserId(entryId, userId).map(this::toDomain);
    }

    @Override
    public Optional<FinancialEntry> findEntryByIdempotencyKey(UUID userId, String idempotencyKey) {
        return entries.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public PageResult<FinancialEntry> findEntries(EntryFilter filter) {
        var pageable = PageRequest.of(
                filter.page(),
                filter.size(),
                Sort.by(Sort.Order.desc("effectiveDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        var page = entries.search(
                filter.userId(), filter.accountId(), filter.categoryId(), filter.type(), filter.status(),
                filter.from(), filter.to(), pageable
        );
        return new PageResult<>(
                page.getContent().stream().map(this::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    @Override
    public List<FinancialEntry> findAccountEntries(UUID userId, UUID accountId) {
        return entries.findAllByUserIdAndAccountIdOrderByEffectiveDateAscCreatedAtAscIdAsc(userId, accountId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<FinancialEntry> findClearedEntries(UUID userId, LocalDate from, LocalDate to) {
        return entries.findAllByUserIdAndStatusAndEffectiveDateBetween(userId, EntryStatus.CLEARED, from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferResult> findTransferByIdempotencyKey(UUID userId, String idempotencyKey) {
        return transfers.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(group -> loadTransfer(userId, group));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferResult> findTransfer(UUID userId, UUID transferId) {
        return transfers.findByIdAndUserId(transferId, userId)
                .map(group -> loadTransfer(userId, group));
    }

    @Override
    public TransferResult saveTransfer(TransferGroup group, FinancialEntry debit, FinancialEntry credit) {
        try {
            return Objects.requireNonNull(writeTransactions.execute(status ->
                    persistTransfer(group, debit, credit)
            ));
        } catch (DataIntegrityViolationException exception) {
            Optional<TransferResult> persisted = findTransferByIdempotencyKey(
                    group.userId(), group.idempotencyKey()
            );
            if (persisted.isEmpty()) {
                throw exception;
            }
            if (!group.requestFingerprint().equals(persisted.get().transfer().requestFingerprint())) {
                throw new BusinessConflictException(
                        "IDEMPOTENCY_CONFLICT",
                        "Idempotency key was already used with another payload"
                );
            }
            return persisted.get();
        }
    }

    @Override
    public TransferResult cancelTransfer(TransferGroup group, FinancialEntry debit, FinancialEntry credit) {
        return Objects.requireNonNull(writeTransactions.execute(status ->
                persistTransfer(group, debit, credit)
        ));
    }

    private TransferResult persistTransfer(
            TransferGroup group,
            FinancialEntry debit,
            FinancialEntry credit
    ) {
        TransferGroup persistedGroup = toDomain(transfers.save(toEntity(group)));
        FinancialEntry persistedDebit = toDomain(entries.save(toEntity(debit)));
        FinancialEntry persistedCredit = toDomain(entries.save(toEntity(credit)));
        return new TransferResult(persistedGroup, persistedDebit, persistedCredit);
    }

    private TransferResult loadTransfer(UUID userId, TransferGroupEntity group) {
        List<FinancialEntry> legs = entries.findAllByUserIdAndTransferGroupId(userId, group.getId())
                .stream().map(this::toDomain).toList();
        FinancialEntry debit = leg(legs, EntryType.TRANSFER_OUT);
        FinancialEntry credit = leg(legs, EntryType.TRANSFER_IN);
        return new TransferResult(toDomain(group), debit, credit);
    }

    private FinancialEntry leg(List<FinancialEntry> legs, EntryType type) {
        return legs.stream().filter(entry -> entry.type() == type).findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer group is incomplete"));
    }

    private AccountEntity toEntity(Account value) {
        return new AccountEntity(
                value.id(), value.userId(), value.name(), value.type(), value.currency(), value.status(),
                value.version(), value.createdAt(), value.updatedAt()
        );
    }

    private Account toDomain(AccountEntity value) {
        return new Account(
                value.getId(), value.getUserId(), value.getName(), value.getType(), value.getCurrency(),
                value.getStatus(), value.getVersion(), value.getCreatedAt(), value.getUpdatedAt()
        );
    }

    private CategoryEntity toEntity(Category value) {
        return new CategoryEntity(
                value.id(), value.userId(), value.name(), value.kind(), value.status(), value.version(),
                value.createdAt(), value.updatedAt()
        );
    }

    private Category toDomain(CategoryEntity value) {
        return new Category(
                value.getId(), value.getUserId(), value.getName(), value.getKind(), value.getStatus(),
                value.getVersion(), value.getCreatedAt(), value.getUpdatedAt()
        );
    }

    private FinancialEntryEntity toEntity(FinancialEntry value) {
        return new FinancialEntryEntity(
                value.id(), value.userId(), value.accountId(), value.categoryId(), value.transferGroupId(),
                value.type(), value.amount(), value.status(), value.effectiveDate(), value.description(),
                value.idempotencyKey(), value.requestFingerprint(), value.version(), value.canceledAt(),
                value.createdAt(), value.updatedAt()
        );
    }

    private FinancialEntry toDomain(FinancialEntryEntity value) {
        return new FinancialEntry(
                value.getId(), value.getUserId(), value.getAccountId(), value.getCategoryId(),
                value.getTransferGroupId(), value.getType(), value.getAmount(), value.getStatus(),
                value.getEffectiveDate(), value.getDescription(), value.getIdempotencyKey(),
                value.getRequestFingerprint(), value.getVersion(), value.getCanceledAt(),
                value.getCreatedAt(), value.getUpdatedAt()
        );
    }

    private TransferGroupEntity toEntity(TransferGroup value) {
        return new TransferGroupEntity(
                value.id(), value.userId(), value.idempotencyKey(), value.requestFingerprint(), value.status(),
                value.cancelReason(), value.canceledAt(), value.version(), value.createdAt(), value.updatedAt()
        );
    }

    private TransferGroup toDomain(TransferGroupEntity value) {
        return new TransferGroup(
                value.getId(), value.getUserId(), value.getIdempotencyKey(),
                value.getRequestFingerprint(), value.getStatus(), value.getCancelReason(), value.getCanceledAt(),
                value.getVersion(), value.getCreatedAt(), value.getUpdatedAt()
        );
    }
}
