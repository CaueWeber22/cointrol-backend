package com.fcproject.infrastructure.config;

import com.fcproject.adapters.outbound.RefreshTokenAdapters;
import com.fcproject.adapters.outbound.UserAdapters;
import com.fcproject.adapters.outbound.FinancePersistenceAdapter;
import com.fcproject.adapters.outbound.persistence.finance.AccountJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.CategoryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.FinancialEntryJPARepository;
import com.fcproject.adapters.outbound.persistence.finance.TransferGroupJPARepository;
import com.fcproject.adapters.outbound.persistence.RefreshTokenJPARepository;
import com.fcproject.adapters.outbound.persistence.RoleJPARepository;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.adapters.outbound.security.BCryptPasswordHasherAdapter;
import com.fcproject.adapters.outbound.security.SpringAuthenticationAdapter;
import com.fcproject.application.core.services.AuthService;
import com.fcproject.application.core.usecases.users.FindUserByEmailUsecase;
import com.fcproject.application.core.usecases.users.FindUserByIdUsecase;
import com.fcproject.application.core.usecases.users.SaveNewUserUsecase;
import com.fcproject.application.core.usecases.finance.FinanceService;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.application.ports.inbound.userPorts.FindUserByEmailInPort;
import com.fcproject.application.ports.inbound.userPorts.FindUserByIdInPort;
import com.fcproject.application.ports.inbound.userPorts.SaveNewUserInPort;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import com.fcproject.application.ports.outbound.PasswordHasherOutPort;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;
import com.fcproject.application.ports.outbound.UserOutPort;
import com.fcproject.application.ports.inbound.finance.FinanceInPort;
import com.fcproject.application.ports.outbound.finance.FinanceOutPort;
import com.fcproject.infrastructure.security.JwtTokenAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
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
    JwtTokenAdapter jwtTokenAdapter(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.access-token-expiration-minutes}") long expirationMinutes
    ) {
        return new JwtTokenAdapter(secret, issuer, audience, expirationMinutes);
    }

    @Bean
    AuthInPort authInPort(
            AuthenticationOutPort authentication,
            RefreshTokenOutPort refreshTokens,
            AccessTokenOutPort accessTokens,
            Clock clock,
            @Value("${security.jwt.refresh-token-expiration-days}") long refreshExpirationDays
    ) {
        return new AuthService(authentication, refreshTokens, accessTokens, clock, refreshExpirationDays);
    }

    @Bean
    FinanceOutPort financeOutPort(
            AccountJPARepository accounts,
            CategoryJPARepository categories,
            FinancialEntryJPARepository entries,
            TransferGroupJPARepository transfers
    ) {
        return new FinancePersistenceAdapter(accounts, categories, entries, transfers);
    }

    @Bean
    FinanceInPort financeInPort(FinanceOutPort finance, Clock clock) {
        return new FinanceService(finance, clock);
    }
}
