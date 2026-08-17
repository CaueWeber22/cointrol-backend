# Arquitetura

## Visão geral

O projeto usa arquitetura hexagonal em um único módulo Maven. O objetivo principal é manter regras de negócio independentes de HTTP, Spring, JPA e provedores de token.

```mermaid
flowchart LR
    HTTP["Controllers e DTOs"] --> IN["Portas de entrada"]
    IN --> USECASES["Casos de uso"]
    USECASES --> DOMAIN["Domínio"]
    USECASES --> OUT["Portas de saída"]
    PERSISTENCE["Adapters JPA"] --> OUT
    AUTH["Adapters JWT / BCrypt / Spring Security"] --> OUT
    CONFIG["ApplicationConfig / SecurityConfig"] -. wiring .-> IN
    CONFIG -. wiring .-> OUT
```

## Responsabilidades

### `application.core`

Contém modelos, comandos, exceções, validações e casos de uso. Não conhece Spring, Jackson, JPA ou classes dos adapters.

### `application.ports.inbound`

Contratos acionados pelos adapters de entrada:

- `AuthInPort`;
- `SaveNewUserInPort`;
- `FindUserByEmailInPort`;
- `FindUserByIdInPort`.

### `application.ports.outbound`

Necessidades externas expressas pelo núcleo:

- persistência de usuários e refresh tokens;
- autenticação de credenciais;
- hash de senha;
- emissão de access token.

### `adapters.inbound`

Controllers REST, validação de payload e conversão entre DTOs e modelos da aplicação.

### `adapters.outbound`

Entidades JPA, repositórios Spring Data e implementações das portas de persistência/segurança.

### `infrastructure`

Composição de beans, filtros HTTP, configuração Spring Security, OpenAPI e tratamento de erros.

## Fluxo de cadastro

```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant SaveUser as SaveNewUserUsecase
    participant Hasher as PasswordHasherOutPort
    participant Users as UserOutPort
    participant DB as PostgreSQL

    Client->>UserController: POST /api/v1/users
    UserController->>SaveUser: CreateUserCommand
    SaveUser->>SaveUser: validar e normalizar e-mail
    SaveUser->>Users: existsByEmail
    SaveUser->>Hasher: hash(senha)
    SaveUser->>Users: save(usuario, hash)
    Users->>DB: INSERT user + ROLE_USER
    DB-->>Client: 201 + perfil sem senha
```

## Fluxo de refresh

```mermaid
sequenceDiagram
    participant Client
    participant Auth as AuthService
    participant Refresh as RefreshTokenOutPort
    participant UserAuth as AuthenticationOutPort
    participant JWT as AccessTokenOutPort

    Client->>Auth: refresh(raw token)
    Auth->>Auth: SHA-256(raw token)
    Auth->>Refresh: findActiveByHash
    Auth->>UserAuth: loadById
    Auth->>JWT: generate
    Auth->>Refresh: rotate com lock
    Auth-->>Client: novo access + novo refresh
```

## Regras automatizadas

`HexagonalArchitectureTest` garante que classes em `application` não dependam de:

- `adapters`;
- `infrastructure`;
- Spring;
- JPA;
- Jackson.

Novas integrações devem começar por uma porta no núcleo e ser implementadas por um adapter externo.

## Convenções

- Injeção por construtor.
- Casos de uso sem anotações de framework.
- Wiring centralizado em classes `@Configuration`.
- Entidades JPA nunca são retornadas pela API.
- DTOs web não atravessam a porta de entrada.
- Horário de autenticação usa `Clock`, permitindo testes determinísticos.
