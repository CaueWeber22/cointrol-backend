# Implementação da fase de estabilização

**Data:** 16 de agosto de 2026
**Objetivo:** transformar a base inicial em um corte vertical executável e testável de cadastro, autenticação, refresh/logout e perfil protegido.

## Resultado

A base agora compila sem o warning estrutural do Lombok, possui testes ativos, valida os limites hexagonais, usa migrations Flyway e oferece um ambiente PostgreSQL local com Docker Compose.

O escopo implementado foi a estabilização do que já existia. As features financeiras novas — contas, categorias, transações, transferências e dashboard — continuam no roadmap porque dependem dessa fundação.

## Alterações realizadas

### Arquitetura

- `AuthService` passou a implementar `AuthInPort` e não importa Spring, JPA, DTOs ou infrastructure.
- Foram criadas portas de saída para autenticação, access token, refresh token e hash de senha.
- JWT, BCrypt e `AuthenticationManager` foram movidos para adapters/configuração.
- Casos de uso são classes Java simples, registrados em `ApplicationConfig`.
- Exceções usadas pelo núcleo foram movidas para `application.core.exceptions`.
- Um teste ArchUnit impede regressões de dependência do núcleo para adapters/frameworks.

### Usuários

- `POST /api/v1/users` foi ativado.
- `GET /api/v1/users/me` retorna o perfil autenticado.
- E-mail é validado e normalizado antes da busca de duplicidade.
- Senha é validada e codificada com BCrypt custo 12 antes de chegar à entidade.
- O hash nunca faz parte do domínio retornado ou dos DTOs de resposta.
- IDs são UUID de ponta a ponta.
- O usuário recebe `ROLE_USER` criado pela migration.

### Autenticação

- Endpoints foram normalizados para `/api/v1/auth`.
- O contrato retorna somente `accessToken`, removendo o alias incorreto `acessToken`.
- Refresh tokens têm 64 bytes aleatórios e somente o SHA-256 é persistido.
- O refresh token é rotacionado sob lock pessimista para impedir duas rotações simultâneas do mesmo registro.
- Token expirado é revogado antes da resposta 401.
- O filtro JWT usa o UUID do `subject`, valida issuer/audience/assinatura e responde 401 em JSON quando o bearer é inválido.
- O segredo JWT é obrigatório no perfil padrão e precisa ter pelo menos 32 bytes.

### API e erros

- DTOs usam Bean Validation e controllers usam `@Valid`.
- Erros seguem `application/problem+json` por meio de `ProblemDetail`.
- Foram padronizados 400, 401, 404, 409 e 500.
- Erros inesperados não expõem mensagem interna ao cliente; o stack trace permanece no log do servidor.
- CORS usa allowlist configurável.
- OpenAPI contém o esquema Bearer e fica desabilitado por padrão, habilitado no perfil local.

### Persistência

- O schema foi padronizado como `access`.
- UUID usa `GenerationType.UUID`, eliminando `uuid identity`.
- `user_roles` passou para o mesmo schema das tabelas relacionadas.
- Booleanos são `NOT NULL` e usam tipo PostgreSQL `BOOLEAN`.
- Auditoria usa `Instant`/`TIMESTAMPTZ` com `created_at` e `updated_at`.
- `ddl-auto` passou de `update` para `validate`.
- Flyway é a única fonte de criação/evolução do banco.
- Foram adicionados constraints, FKs e índices para refresh tokens.

### Build e operação

- Lombok, Dozer, Jackson XML e dependências diretas redundantes foram removidos.
- Actuator expõe somente `health` e `info`.
- JaCoCo gera relatório durante `verify`.
- Surefire falha se não encontrar testes.
- `compose.yml` sobe PostgreSQL local com health check e volume persistente.
- `.env.example` documenta todas as variáveis.
- `.github/workflows/ci.yml` executa Java 21 e `mvnw verify`.

## Testes adicionados

- validação e normalização de usuário;
- hash antes da persistência e conflito de e-mail;
- login, rotação e expiração de refresh token;
- serialização do contrato de tokens;
- validação HTTP de login e cadastro;
- ausência de senha/hash em respostas;
- regras de arquitetura com ArchUnit;
- migrations contra PostgreSQL real com Testcontainers.

Resultado local da implementação:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

O teste ignorado foi o de PostgreSQL/Testcontainers porque o Docker daemon não estava ativo no ambiente de validação. Ele é habilitado automaticamente quando Docker está disponível e deve rodar no CI.

Além da suíte, o contexto Spring web foi inicializado com persistência isolada para validar o grafo completo de beans. Requisições reais confirmaram `400 application/problem+json` para login inválido e `401 application/problem+json` para perfil sem autenticação.

O relatório JaCoCo da validação registrou 32,2% de instruções e 26,9% de branches. O build agora impõe um piso inicial de 30%/25% para impedir regressão silenciosa. Esse piso não é a meta final: a cobertura deve subir junto dos testes JPA/PostgreSQL e das próximas features.

## Compatibilidade e mudanças de contrato

As mudanças abaixo são intencionalmente incompatíveis com o protótipo anterior:

| Antes | Agora |
|---|---|
| `/auth/login` | `/api/v1/auth/login` |
| `/auth/refresh` | `/api/v1/auth/refresh` |
| `/auth/logout` | `/api/v1/auth/logout` |
| `acessToken` e `accessToken` | somente `accessToken` |
| schema `acess` | schema `access` |
| coluna `password` | coluna `password_hash` |
| Hibernate `ddl-auto: update` | Flyway + `ddl-auto: validate` |

Como ainda não havia API financeira publicada nem migration versionada, a correção foi feita diretamente, sem manter aliases inseguros ou grafias incorretas.

## Pendências deliberadas

- rate limiting de login e refresh;
- recuperação e verificação de e-mail;
- família de sessões e detecção global de replay de refresh token;
- endpoint de revogação de todas as sessões;
- limpeza periódica de refresh tokens expirados;
- observabilidade avançada, logs estruturados e correlation ID;
- scanner de vulnerabilidades e secret scanning;
- primeira feature financeira: conta + lançamento + saldo.

Esses itens permanecem priorizados no [roadmap](AVALIACAO_TECNICA_E_ROADMAP.md).
