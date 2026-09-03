package com.fcproject.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcproject.adapters.outbound.RefreshTokenAdapters;
import com.fcproject.adapters.outbound.LoginAttemptPersistenceAdapter;
import com.fcproject.adapters.outbound.SecurityAuditPersistenceAdapter;
import com.fcproject.adapters.outbound.UserAdapters;
import com.fcproject.adapters.outbound.FinancePersistenceAdapter;
import com.fcproject.adapters.outbound.persistence.finance.AccountJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.CategoryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.FinancialEntryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.TransferGroupJPARepository;
import com.fcproject.adapters.outbound.persistence.RefreshTokenJPARepository;
import com.fcproject.adapters.outbound.persistence.SecurityAuditEventJPARepository;
import com.fcproject.adapters.outbound.persistence.RoleJPARepository;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.adapters.outbound.security.BCryptPasswordHasherAdapter;
import com.fcproject.adapters.outbound.security.SpringAuthenticationAdapter;
import com.fcproject.application.core.services.AuthService;
import com.fcproject.application.core.usecases.finance.accounts.ArchiveAccountUsecase;
import com.fcproject.application.core.usecases.finance.accounts.CreateAccountUsecase;
import com.fcproject.application.core.usecases.finance.accounts.GetAccountBalanceUsecase;
import com.fcproject.application.core.usecases.finance.accounts.GetAccountUsecase;
import com.fcproject.application.core.usecases.finance.accounts.ListAccountsUsecase;
import com.fcproject.application.core.usecases.finance.accounts.UpdateAccountUsecase;
import com.fcproject.application.core.usecases.finance.categories.ArchiveCategoryUsecase;
import com.fcproject.application.core.usecases.finance.categories.CreateCategoryUsecase;
import com.fcproject.application.core.usecases.finance.categories.ListCategoriesUsecase;
import com.fcproject.application.core.usecases.finance.categories.UpdateCategoryUsecase;
import com.fcproject.application.core.usecases.finance.entries.CancelEntryUsecase;
import com.fcproject.application.core.usecases.finance.entries.CreateEntryUsecase;
import com.fcproject.application.core.usecases.finance.entries.GetEntryUsecase;
import com.fcproject.application.core.usecases.finance.entries.ListEntriesUsecase;
import com.fcproject.application.core.usecases.finance.entries.UpdateEntryUsecase;
import com.fcproject.application.core.usecases.finance.summaries.SummarizeByCategoryUsecase;
import com.fcproject.application.core.usecases.finance.summaries.SummarizeTimelineUsecase;
import com.fcproject.application.core.usecases.finance.summaries.SummarizeUsecase;
import com.fcproject.application.core.usecases.finance.transfers.CancelTransferUsecase;
import com.fcproject.application.core.usecases.finance.transfers.CreateTransferUsecase;
import com.fcproject.application.core.usecases.finance.transfers.GetTransferUsecase;
import com.fcproject.application.core.usecases.users.FindUserByEmailUsecase;
import com.fcproject.application.core.usecases.users.FindUserByIdUsecase;
import com.fcproject.application.core.usecases.users.SaveNewUserUsecase;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.application.ports.inbound.finance.ArchiveAccountInPort;
import com.fcproject.application.ports.inbound.finance.ArchiveCategoryInPort;
import com.fcproject.application.ports.inbound.finance.CancelEntryInPort;
import com.fcproject.application.ports.inbound.finance.CancelTransferInPort;
import com.fcproject.application.ports.inbound.finance.CreateAccountInPort;
import com.fcproject.application.ports.inbound.finance.CreateCategoryInPort;
import com.fcproject.application.ports.inbound.finance.CreateEntryInPort;
import com.fcproject.application.ports.inbound.finance.CreateTransferInPort;
import com.fcproject.application.ports.inbound.finance.GetAccountBalanceInPort;
import com.fcproject.application.ports.inbound.finance.GetAccountInPort;
import com.fcproject.application.ports.inbound.finance.GetEntryInPort;
import com.fcproject.application.ports.inbound.finance.GetTransferInPort;
import com.fcproject.application.ports.inbound.finance.ListAccountsInPort;
import com.fcproject.application.ports.inbound.finance.ListCategoriesInPort;
import com.fcproject.application.ports.inbound.finance.ListEntriesInPort;
import com.fcproject.application.ports.inbound.finance.SummarizeByCategoryInPort;
import com.fcproject.application.ports.inbound.finance.SummarizeInPort;
import com.fcproject.application.ports.inbound.finance.SummarizeTimelineInPort;
import com.fcproject.application.ports.inbound.finance.UpdateAccountInPort;
import com.fcproject.application.ports.inbound.finance.UpdateCategoryInPort;
import com.fcproject.application.ports.inbound.finance.UpdateEntryInPort;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import com.fcproject.application.ports.inbound.userPorts.FindUserByIdInPort;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import com.fcproject.application.ports.outbound.LoginAttemptOutPort;
import com.fcproject.application.ports.outbound.PasswordHasherOutPort;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;
import com.fcproject.application.ports.outbound.UserOutPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;
import com.fcproject.infrastructure.security.JwtTokenAdapter;
import com.fcproject.infrastructure.security.FixedWindowRateLimiter;
import com.fcproject.infrastructure.security.RateLimitFilter;
import com.fcproject.infrastructure.security.RateLimitPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

@Configuration
@EnableScheduling
public class ApplicationConfig {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    UserOutPort userOutPort(UserJPARepository users, RoleJPARepository roles) {
        return new UserAdapters(users, roles);
    }

    @Bean
    PasswordHasherOutPort passwordHasherOutPort(PasswordEncoder passwordEncoder) {
        return new BCryptPasswordHasherAdapter(passwordEncoder);
    }

    @Bean
    SaveNewUserInPort saveNewUserInPort(UserOutPort users, PasswordHasherOutPort passwordHasher) {
        return new SaveNewUserUsecase(users, passwordHasher);
    }

    @Bean
    FindUserByEmailInPort findUserByEmailInPort(UserOutPort users) {
        return new FindUserByEmailUsecase(users);
    }

    @Bean
    FindUserByIdInPort findUserByIdInPort(UserOutPort users) {
        return new FindUserByIdUsecase(users);
    }

    @Bean
    AuthenticationOutPort authenticationOutPort(
            AuthenticationManager authenticationManager,
            UserJPARepository users
    ) {
        return new SpringAuthenticationAdapter(authenticationManager, users);
    }

    @Bean
    RefreshTokenOutPort refreshTokenOutPort(
            RefreshTokenJPARepository refreshTokens,
            UserJPARepository users
    ) {
        return new RefreshTokenAdapters(refreshTokens, users);
    }

    @Bean
    LoginAttemptOutPort loginAttemptOutPort(JdbcTemplate jdbcTemplate) {
        return new LoginAttemptPersistenceAdapter(jdbcTemplate);
    }

    @Bean
    SecurityAuditOutPort securityAuditOutPort(SecurityAuditEventJPARepository events) {
        return new SecurityAuditPersistenceAdapter(events);
    }

    @Bean
    JwtTokenAdapter jwtTokenAdapter(
            @Value("${security.jwt.active-key-id}") String activeKeyId,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.previous-keys:}") String previousKeys,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.access-token-expiration-minutes}") long expirationMinutes
    ) {
        return new JwtTokenAdapter(
                activeKeyId, secret, previousKeys, issuer, audience, expirationMinutes
        );
    }

    @Bean
    AuthInPort authInPort(
            AuthenticationOutPort authentication,
            RefreshTokenOutPort refreshTokens,
            AccessTokenOutPort accessTokens,
            LoginAttemptOutPort loginAttempts,
            SecurityAuditOutPort securityAudit,
            Clock clock,
            @Value("${security.jwt.refresh-token-expiration-days}") long refreshExpirationDays,
            @Value("${security.login-protection.maximum-attempts}") int maximumLoginAttempts,
            @Value("${security.login-protection.attempt-window-seconds}") long attemptWindowSeconds,
            @Value("${security.login-protection.lock-duration-seconds}") long lockDurationSeconds
    ) {
        return new AuthService(
                authentication,
                refreshTokens,
                accessTokens,
                loginAttempts,
                securityAudit,
                clock,
                refreshExpirationDays,
                maximumLoginAttempts,
                Duration.ofSeconds(attemptWindowSeconds),
                Duration.ofSeconds(lockDurationSeconds)
        );
    }

    @Bean
    RateLimitFilter rateLimitFilter(
            ObjectMapper objectMapper,
            SecurityAuditOutPort securityAudit,
            Clock clock,
            @Value("${security.rate-limit.enabled}") boolean enabled,
            @Value("${security.rate-limit.maximum-tracked-keys}") int maximumTrackedKeys,
            @Value("${security.rate-limit.login.requests}") int loginRequests,
            @Value("${security.rate-limit.login.window-seconds}") long loginWindowSeconds,
            @Value("${security.rate-limit.refresh.requests}") int refreshRequests,
            @Value("${security.rate-limit.refresh.window-seconds}") long refreshWindowSeconds,
            @Value("${security.rate-limit.registration.requests}") int registrationRequests,
            @Value("${security.rate-limit.registration.window-seconds}") long registrationWindowSeconds,
            @Value("${security.rate-limit.api.requests}") int apiRequests,
            @Value("${security.rate-limit.api.window-seconds}") long apiWindowSeconds
    ) {
        Map<String, RateLimitPolicy> exactPolicies = Map.of(
                "POST /api/v1/auth/login",
                new RateLimitPolicy("login", loginRequests, Duration.ofSeconds(loginWindowSeconds)),
                "POST /api/v1/auth/refresh",
                new RateLimitPolicy("refresh", refreshRequests, Duration.ofSeconds(refreshWindowSeconds)),
                "POST /api/v1/users",
                new RateLimitPolicy("registration", registrationRequests, Duration.ofSeconds(registrationWindowSeconds))
        );
        return new RateLimitFilter(
                objectMapper,
                securityAudit,
                clock,
                new FixedWindowRateLimiter(maximumTrackedKeys),
                enabled,
                exactPolicies,
                new RateLimitPolicy("api", apiRequests, Duration.ofSeconds(apiWindowSeconds))
        );
    }

    @Bean
    FinanceOutPort financeOutPort(
            AccountJPARepository accounts,
            CategoryJPARepository categories,
            FinancialEntryJPARepository entries,
            TransferGroupJPARepository transfers,
            PlatformTransactionManager transactionManager
    ) {
        return new FinancePersistenceAdapter(
                accounts, categories, entries, transfers, new TransactionTemplate(transactionManager)
        );
    }

    @Bean
    CreateAccountInPort createAccountInPort(FinanceOutPort finance, Clock clock) {
        return new CreateAccountUsecase(finance, clock);
    }

    @Bean
    ListAccountsInPort listAccountsInPort(FinanceOutPort finance) {
        return new ListAccountsUsecase(finance);
    }

    @Bean
    GetAccountInPort getAccountInPort(FinanceOutPort finance) {
        return new GetAccountUsecase(finance);
    }

    @Bean
    UpdateAccountInPort updateAccountInPort(FinanceOutPort finance, Clock clock) {
        return new UpdateAccountUsecase(finance, clock);
    }

    @Bean
    ArchiveAccountInPort archiveAccountInPort(FinanceOutPort finance, Clock clock) {
        return new ArchiveAccountUsecase(finance, clock);
    }

    @Bean
    GetAccountBalanceInPort getAccountBalanceInPort(FinanceOutPort finance) {
        return new GetAccountBalanceUsecase(finance);
    }

    @Bean
    CreateCategoryInPort createCategoryInPort(FinanceOutPort finance, Clock clock) {
        return new CreateCategoryUsecase(finance, clock);
    }

    @Bean
    ListCategoriesInPort listCategoriesInPort(FinanceOutPort finance) {
        return new ListCategoriesUsecase(finance);
    }

    @Bean
    UpdateCategoryInPort updateCategoryInPort(FinanceOutPort finance, Clock clock) {
        return new UpdateCategoryUsecase(finance, clock);
    }

    @Bean
    ArchiveCategoryInPort archiveCategoryInPort(FinanceOutPort finance, Clock clock) {
        return new ArchiveCategoryUsecase(finance, clock);
    }

    @Bean
    CreateEntryInPort createEntryInPort(FinanceOutPort finance, Clock clock) {
        return new CreateEntryUsecase(finance, clock);
    }

    @Bean
    GetEntryInPort getEntryInPort(FinanceOutPort finance) {
        return new GetEntryUsecase(finance);
    }

    @Bean
    ListEntriesInPort listEntriesInPort(FinanceOutPort finance) {
        return new ListEntriesUsecase(finance);
    }

    @Bean
    UpdateEntryInPort updateEntryInPort(FinanceOutPort finance, Clock clock) {
        return new UpdateEntryUsecase(finance, clock);
    }

    @Bean
    CancelEntryInPort cancelEntryInPort(FinanceOutPort finance, Clock clock) {
        return new CancelEntryUsecase(finance, clock);
    }

    @Bean
    CreateTransferInPort createTransferInPort(FinanceOutPort finance, Clock clock) {
        return new CreateTransferUsecase(finance, clock);
    }

    @Bean
    GetTransferInPort getTransferInPort(FinanceOutPort finance) {
        return new GetTransferUsecase(finance);
    }

    @Bean
    CancelTransferInPort cancelTransferInPort(FinanceOutPort finance, Clock clock) {
        return new CancelTransferUsecase(finance, clock);
    }

    @Bean
    SummarizeInPort summarizeInPort(FinanceOutPort finance) {
        return new SummarizeUsecase(finance);
    }

    @Bean
    SummarizeByCategoryInPort summarizeByCategoryInPort(FinanceOutPort finance) {
        return new SummarizeByCategoryUsecase(finance);
    }

    @Bean
    SummarizeTimelineInPort summarizeTimelineInPort(FinanceOutPort finance) {
        return new SummarizeTimelineUsecase(finance);
    }

}
