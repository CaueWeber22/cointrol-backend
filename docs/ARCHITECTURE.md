# Arquitetura do backend

Este arquivo é o mapa curto para agentes. A documentação técnica mais completa continua em [ARQUITETURA.md](ARQUITETURA.md).

## Visão geral

O backend do Cointrol é um único módulo Maven com Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway, Spring Security, JWT, JPA, ArchUnit e Testcontainers.

O desenho principal é hexagonal: o núcleo da aplicação descreve regras e portas; os detalhes de HTTP, banco, segurança e framework ficam em adapters e infraestrutura.

## Camadas

`application/core`

Contém domínio, comandos, casos de uso, serviços de aplicação, validações e exceções de negócio. Esta camada deve continuar livre de dependências de Spring MVC, JPA, Jackson, controllers, DTOs HTTP e infraestrutura.

`application/ports/inbound`

Define os contratos acionados pela borda de entrada. Cada fluxo de negócio relevante deve ter uma porta de entrada clara.

`application/ports/outbound`

Define as capacidades externas necessárias pelo núcleo, como persistência, autenticação, emissão de tokens, hash de senha, auditoria e leitura do usuário autenticado.

`adapters/inbound`

Contém controllers REST, DTOs de request/response e tradução entre HTTP e portas de entrada. Controllers devem ser finos: validar formato, resolver usuário autenticado, montar comandos e delegar.

`adapters/outbound`

Contém entities JPA, repositories Spring Data, mappers e implementações de portas de saída. Esta camada pode conhecer banco e framework; o núcleo não.

`infrastructure`

Contém wiring Spring, filtros, configuração de segurança, JWT, rate limiting, exception handler, Swagger/OpenAPI e jobs de retenção.

## Banco de dados

O banco suportado é PostgreSQL. Flyway é a fonte de verdade da estrutura em `src/main/resources/db/migration`; Hibernate apenas valida com `ddl-auto=validate`.

Schemas principais:

- `access`: usuários, papéis, refresh tokens, tentativas de login e auditoria de segurança.
- `finance`: contas, categorias, lançamentos financeiros e grupos de transferência.

Alterações de schema devem consultar [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md).

## Fluxos principais

Cadastro:

1. `UserController` recebe o payload.
2. O controller cria `CreateUserCommand`.
3. `SaveNewUserUsecase` normaliza e valida dados.
4. O núcleo verifica duplicidade de e-mail, solicita hash de senha e persiste o usuário por porta de saída.
5. A resposta pública nunca expõe senha ou hash.

Autenticação:

1. `AuthController` delega para `AuthService`.
2. E-mail é normalizado e usado em proteção de login.
3. Falhas incrementam contador persistente e geram auditoria.
4. Sucesso gera access token JWT e refresh token opaco.
5. Refresh token é armazenado apenas como SHA-256 e rotacionado a cada uso.
6. Login/refresh retornam 204 e cookies HttpOnly, Secure, SameSite=None; não expõem tokens no JSON.
7. Toda mutação exige CSRF sincronizado por sessão. GET /api/v1/auth/csrf fornece o token; login/refresh invalidam o anterior. A sessão não armazena autenticação.
8. O filtro aceita access_token ou Authorization explícito, que tem precedência. Consulte [FRONTEND_AUTH_COOKIES.md](FRONTEND_AUTH_COOKIES.md).

Financeiro:

1. Controller resolve o `userId` autenticado.
2. Use case valida propriedade, status, moeda, idempotência e regra de domínio.
3. Adapter persiste via JPA e respeita transações necessárias.
4. Recursos de outro usuário devem aparecer como inexistentes, não como acessíveis.

Transferência:

1. O núcleo valida contas diferentes, ativas, do mesmo usuário e mesma moeda.
2. Cria um grupo de transferência e duas pernas: `TRANSFER_OUT` e `TRANSFER_IN`.
3. O adapter persiste grupo e pernas em uma única transação.
4. Cancelamento acontece pelo grupo e cancela as duas pernas junto.

## Regras arquiteturais

- Entidades JPA não atravessam a API.
- DTOs HTTP não atravessam portas de entrada.
- Use cases não devem conhecer controllers, repositories ou annotations de web/persistência.
- Novas integrações externas começam como portas no núcleo e adapters na borda.
- Regras financeiras devem ser testadas em use cases, não só em controllers.
- `HexagonalArchitectureTest` deve continuar protegendo os limites do núcleo.
