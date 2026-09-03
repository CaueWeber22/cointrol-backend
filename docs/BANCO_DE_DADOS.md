# Banco de dados e migrations

## Tecnologia e propriedade do schema

O banco suportado é PostgreSQL. O Flyway é responsável pela estrutura; o Hibernate apenas valida o resultado com `ddl-auto: validate`.

O schema `access` contém identidade e sessões. O schema `finance` contém contas, categorias, lançamentos e transferências. O nome anterior `acess` foi corrigido.

## Scripts versionados

| Versão | Arquivo | Função |
|---|---|---|
| V1 | `src/main/resources/db/migration/V1__create_access_schema.sql` | Cria schema, usuários, papéis, associação e refresh tokens. |
| V2 | `src/main/resources/db/migration/V2__seed_default_roles.sql` | Insere `ROLE_USER` e `ROLE_ADMIN`. |
| V3 | `src/main/resources/db/migration/V3__create_access_indexes.sql` | Cria índices de consulta/limpeza de refresh tokens. |
| V4 | `src/main/resources/db/migration/V4__create_finance_schema.sql` | Cria o schema financeiro. |
| V5 | `src/main/resources/db/migration/V5__create_accounts.sql` | Cria contas, constraints e índices por proprietário. |
| V6 | `src/main/resources/db/migration/V6__create_categories.sql` | Cria categorias e unicidade por usuário/tipo. |
| V7 | `src/main/resources/db/migration/V7__create_financial_entries.sql` | Cria a razão de lançamentos, idempotência e índices do extrato. |
| V8 | `src/main/resources/db/migration/V8__create_transfer_groups.sql` | Cria grupos de transferência e o vínculo atômico das pernas. |
| V9 | `src/main/resources/db/migration/V9__add_transfer_cancellation.sql` | Adiciona estado, motivo, instante de cancelamento, versão e auditoria ao grupo. |
| V10 | `src/main/resources/db/migration/V10__align_account_currency_type.sql` | Alinha `accounts.currency` de `CHAR(3)` para `VARCHAR(3)`, conforme o mapeamento JPA. |
| V11 | `src/main/resources/db/migration/V11__add_security_controls.sql` | Cria proteção persistente de login e auditoria de eventos de segurança. |

O Flyway mantém `access.flyway_schema_history` e aplica cada versão uma única vez.

## Modelo atual

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : grants
    USERS ||--o{ REFRESH_TOKENS : owns
    USERS ||--o{ SECURITY_AUDIT_EVENTS : referenced_by
    USERS ||--o{ ACCOUNTS : owns
    USERS ||--o{ CATEGORIES : owns
    USERS ||--o{ FINANCIAL_ENTRIES : owns
    ACCOUNTS ||--o{ FINANCIAL_ENTRIES : contains
    CATEGORIES ||--o{ FINANCIAL_ENTRIES : classifies
    TRANSFER_GROUPS ||--|{ FINANCIAL_ENTRIES : links

    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        date date_of_birth
        timestamptz created_at
        timestamptz updated_at
        boolean enabled
    }
    ROLES {
        uuid id PK
        varchar name UK
        varchar description
    }
    USER_ROLES {
        uuid user_id FK
        uuid role_id FK
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        timestamptz expires_at
        timestamptz revoked_at
    }
    LOGIN_ATTEMPTS {
        varchar identifier_hash PK
        int failed_attempts
        timestamptz window_started_at
        timestamptz locked_until
        timestamptz updated_at
    }
    SECURITY_AUDIT_EVENTS {
        uuid id PK
        uuid user_id FK
        varchar identifier_hash
        varchar event_type
        varchar client_ip
        timestamptz occurred_at
    }
    ACCOUNTS {
        uuid id PK
        uuid user_id FK
        varchar name
        varchar type
        varchar currency
        varchar status
        bigint version
    }
    CATEGORIES {
        uuid id PK
        uuid user_id FK
        varchar name
        varchar kind
        varchar status
        bigint version
    }
    FINANCIAL_ENTRIES {
        uuid id PK
        uuid user_id FK
        uuid account_id FK
        uuid category_id FK
        uuid transfer_group_id FK
        varchar type
        numeric amount
        varchar status
        date effective_date
        varchar idempotency_key
    }
    TRANSFER_GROUPS {
        uuid id PK
        uuid user_id FK
        varchar idempotency_key
        varchar request_fingerprint
        varchar status
        varchar cancel_reason
        timestamptz canceled_at
        bigint version
        timestamptz updated_at
    }
```

## Integridade financeira

- Valores usam `NUMERIC(19,4)` e precisam ser positivos.
- O tipo determina se o lançamento soma ou subtrai do saldo.
- Não existe coluna materializada de saldo.
- FKs compostas por recurso e `user_id` impedem vínculos entre proprietários diferentes.
- Índices parciais garantem nomes ativos únicos e idempotency keys únicas.
- Lançamentos cancelados exigem `canceled_at`; os demais proíbem esse campo.
- `TRANSFER_IN` e `TRANSFER_OUT` exigem `transfer_group_id`.
- Conta, grupo e duas pernas de transferência são gravados em transações atômicas no adapter.
- Cancelamento exige motivo e instante quando o grupo está `CANCELED`; grupos `COMPLETED` proíbem esses campos.
- Cancelar uma transferência atualiza o grupo e as duas pernas na mesma transação.

## Subir o PostgreSQL local

```powershell
docker compose up -d postgres
docker compose ps
```

O volume `cointrol-postgres-data` preserva dados entre reinicializações.

Depois, iniciar a aplicação:

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

As migrations são aplicadas automaticamente antes da criação do `EntityManagerFactory`.

## Variáveis

| Variável | Descrição |
|---|---|
| `PG_DB_URL` | URL JDBC completa. |
| `PG_DB_USERNAME` | Usuário PostgreSQL. |
| `PG_DB_PASSWORD` | Senha PostgreSQL. |
| `POSTGRES_DB` | Banco criado pelo container. |
| `POSTGRES_USER` | Usuário criado pelo container. |
| `POSTGRES_PASSWORD` | Senha usada pelo container. |

Os valores locais estão em `application-local.yml` e `.env.example`; não devem ser reutilizados em produção.

## Banco preexistente no schema `acess`

As migrations atuais assumem um banco novo. Se já houver dados reais no schema antigo `acess`, **não renomeie nem apague tabelas manualmente**.

Procedimento recomendado:

1. Fazer backup verificado.
2. Inventariar tabelas, colunas, constraints e volume de dados.
3. Criar uma migration específica de transição para `access`.
4. Converter `password` somente se já contiver BCrypt; senha em texto puro exige reset seguro, não migração direta.
5. Validar contagens e FKs em ambiente de cópia.
6. Planejar rollback antes da aplicação em produção.

Essa migration não foi automatizada porque o repositório não informa se existe banco com dados nem qual é seu conteúdo.

## Política para novas migrations

- Nunca editar uma migration já aplicada em ambiente compartilhado.
- Criar sempre a próxima versão `Vn__descricao.sql`.
- Toda migration deve funcionar a partir de um banco vazio no teste Testcontainers.
- Alterações destrutivas exigem estratégia em duas etapas e backup.
- Índices grandes devem considerar criação concorrente e janela operacional.
- Dados de referência devem usar IDs estáveis e operações idempotentes quando apropriado.

## Teste automatizado

`DatabaseMigrationTest` cria um PostgreSQL 17 vazio, aplica as onze migrations e valida tabelas, papéis, FKs, controles de segurança, colunas de cancelamento e o tipo de `accounts.currency` nos schemas `access` e `finance`.

```powershell
docker info
.\mvnw.cmd test
```

Sem Docker ativo, somente esse teste é ignorado; no CI ele deve ser executado.
