package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.security.CurrentUserIdProvider;
import com.fcproject.application.core.domain.finance.FinanceModels.Account;
import com.fcproject.application.core.domain.finance.FinanceModels.AccountType;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import com.fcproject.application.core.domain.finance.FinanceModels.FinancialEntry;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferGroup;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferResult;
import com.fcproject.application.core.domain.finance.FinanceModels.TransferStatus;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import com.fcproject.infrastructure.exceptions.GlobalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ENTRY_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");
    private static final Principal PRINCIPAL = () -> "ada@example.com";

    @Mock
    private FinanceInPort finance;

    @Mock
    private CurrentUserIdProvider currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(
                new AccountController(finance, currentUser),
                new TransactionController(finance, currentUser),
                new TransferController(finance, currentUser)
        ).setControllerAdvice(new GlobalHandler()).setValidator(validator).build();
        lenient().when(currentUser.get(any(Principal.class))).thenReturn(USER_ID);
    }

    @Test
    void createsAccountWithoutAcceptingOwnerFromPayload() throws Exception {
        when(finance.createAccount(any())).thenReturn(new Account(
                ACCOUNT_ID, USER_ID, "Conta principal", AccountType.CHECKING, "BRL",
                ResourceStatus.ACTIVE, 0, NOW, NOW
        ));

        mockMvc.perform(post("/api/v1/accounts")
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Conta principal",
                                  "type": "CHECKING",
                                  "currency": "BRL",
                                  "userId": "ffffffff-ffff-ffff-ffff-ffffffffffff"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/accounts/" + ACCOUNT_ID))
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void validatesAccountPayload() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\", \"type\":null, \"currency\":\"REAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createsTransactionWithIdempotencyHeader() throws Exception {
        when(finance.createEntry(any())).thenReturn(entry(ENTRY_ID, ACCOUNT_ID, null, EntryType.EXPENSE));

        mockMvc.perform(post("/api/v1/transactions")
                        .principal(PRINCIPAL)
                        .header("Idempotency-Key", "entry-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "20000000-0000-0000-0000-000000000001",
                                  "categoryId": "30000000-0000-0000-0000-000000000001",
                                  "type": "EXPENSE",
                                  "amount": 49.90,
                                  "status": "CLEARED",
                                  "effectiveDate": "2026-08-16",
                                  "description": "Mercado"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/transactions/" + ENTRY_ID))
                .andExpect(jsonPath("$.amount").value(49.9))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        verify(finance).createEntry(any());
    }

    @Test
    void createsTransferWithTwoLinkedLegs() throws Exception {
        UUID groupId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        FinancialEntry debit = entry(UUID.randomUUID(), ACCOUNT_ID, groupId, EntryType.TRANSFER_OUT);
        FinancialEntry credit = entry(UUID.randomUUID(), SECOND_ACCOUNT_ID, groupId, EntryType.TRANSFER_IN);
        when(finance.transfer(any())).thenReturn(new TransferResult(
                new TransferGroup(
                        groupId, USER_ID, "transfer-1", "fingerprint", TransferStatus.COMPLETED,
                        null, null, 0, NOW, NOW
                ), debit, credit
        ));

        mockMvc.perform(post("/api/v1/transfers")
                        .principal(PRINCIPAL)
                        .header("Idempotency-Key", "transfer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "20000000-0000-0000-0000-000000000001",
                                  "destinationAccountId": "20000000-0000-0000-0000-000000000002",
                                  "amount": 100.00,
                                  "effectiveDate": "2026-08-16",
                                  "description": "Reserva"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/transfers/" + groupId))
                .andExpect(jsonPath("$.debit.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.credit.type").value("TRANSFER_IN"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getsAndCancelsTransferGroup() throws Exception {
        UUID groupId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        FinancialEntry debit = entry(UUID.randomUUID(), ACCOUNT_ID, groupId, EntryType.TRANSFER_OUT);
        FinancialEntry credit = entry(UUID.randomUUID(), SECOND_ACCOUNT_ID, groupId, EntryType.TRANSFER_IN);
        TransferResult completed = new TransferResult(
                new TransferGroup(
                        groupId, USER_ID, "transfer-1", "fingerprint", TransferStatus.COMPLETED,
                        null, null, 0, NOW, NOW
                ), debit, credit
        );
        TransferResult canceled = new TransferResult(
                new TransferGroup(
                        groupId, USER_ID, "transfer-1", "fingerprint", TransferStatus.CANCELED,
                        "conta incorreta", NOW, 1, NOW, NOW
                ), canceled(debit), canceled(credit)
        );
        when(finance.getTransfer(USER_ID, groupId)).thenReturn(completed);
        when(finance.cancelTransfer(USER_ID, groupId, "conta incorreta")).thenReturn(canceled);

        mockMvc.perform(get("/api/v1/transfers/{id}", groupId).principal(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/transfers/{id}/cancel", groupId)
                        .principal(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"conta incorreta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.cancelReason").value("conta incorreta"))
                .andExpect(jsonPath("$.canceledAt").exists())
                .andExpect(jsonPath("$.debit.status").value("CANCELED"))
                .andExpect(jsonPath("$.credit.status").value("CANCELED"));
    }

    private FinancialEntry entry(UUID id, UUID accountId, UUID groupId, EntryType type) {
        return new FinancialEntry(
                id, USER_ID, accountId, type == EntryType.EXPENSE ? CATEGORY_ID : null, groupId,
                type, new BigDecimal(type == EntryType.EXPENSE ? "49.9000" : "100.0000"),
                EntryStatus.CLEARED, LocalDate.of(2026, 8, 16), "Teste", null, null,
                0, null, NOW, NOW
        );
    }

    private FinancialEntry canceled(FinancialEntry entry) {
        return new FinancialEntry(
                entry.id(), entry.userId(), entry.accountId(), entry.categoryId(), entry.transferGroupId(),
                entry.type(), entry.amount(), EntryStatus.CANCELED, entry.effectiveDate(), entry.description(),
                entry.idempotencyKey(), entry.requestFingerprint(), entry.version(), NOW,
                entry.createdAt(), NOW
        );
    }
}
