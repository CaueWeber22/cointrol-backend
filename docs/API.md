# API v1

Base path: `/api/v1`
Formato: JSON
Autenticação: `Authorization: Bearer <accessToken>`

## Cadastro

`POST /api/v1/users`

```json
{
  "first_name": "Ada",
  "last_name": "Lovelace",
  "email": "ada@example.com",
  "phone": "+55 11 99999-9999",
  "gender": "female",
  "date_of_birth": "1990-01-01",
  "password": "Valid@123"
}
```

Resposta `201 Created`:

```json
{
  "id": "10000000-0000-0000-0000-000000000001",
  "first_name": "Ada",
  "last_name": "Lovelace",
  "email": "ada@example.com",
  "phone": "+55 11 99999-9999",
  "gender": "FEMALE",
  "date_of_birth": "1990-01-01"
}
```

O header `Location` aponta para `/api/v1/users/me`.

## Login

`POST /api/v1/auth/login`

```json
{
  "email": "ada@example.com",
  "password": "Valid@123"
}
```

Resposta `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-token>",
  "expiresIn": 900,
  "type": "Bearer"
}
```

`expiresIn` é expresso em segundos.

## Renovar sessão

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "<opaque-token>"
}
```

Retorna um novo par de tokens. O refresh token anterior é revogado e não deve ser reutilizado.

## Logout

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "<opaque-token>"
}
```

Resposta: `204 No Content`. A operação é idempotente para tokens já revogados ou desconhecidos.

## Perfil autenticado

`GET /api/v1/users/me`

Requer access token válido. Retorna o mesmo contrato público de usuário do cadastro.

## Contas financeiras

Todos os endpoints desta seção exigem access token. O proprietário é obtido do token e não deve ser enviado no payload.

### Criar conta

`POST /api/v1/accounts`

```json
{
  "name": "Conta principal",
  "type": "CHECKING",
  "currency": "BRL",
  "openingBalance": 1500.00
}
```

`openingBalance` é opcional. Quando informado, é persistido atomicamente como lançamento `OPENING_BALANCE`; não existe campo de saldo editável.

Tipos: `CHECKING`, `SAVINGS`, `CASH` e `INVESTMENT`.

Resposta `201 Created`, com `Location: /api/v1/accounts/{id}`.

### Consultar e manter contas

| Método | Endpoint | Função |
|---|---|---|
| `GET` | `/api/v1/accounts?status=ACTIVE` | Lista contas, opcionalmente por `ACTIVE` ou `ARCHIVED`. |
| `GET` | `/api/v1/accounts/{id}` | Retorna uma conta do usuário. |
| `PATCH` | `/api/v1/accounts/{id}` | Renomeia usando `{"name":"Novo nome"}`. |
| `DELETE` | `/api/v1/accounts/{id}` | Arquiva de forma idempotente e retorna `204`. |
| `GET` | `/api/v1/accounts/{id}/balance` | Retorna saldos confirmado, pendente e projetado. |

Exemplo de saldo:

```json
{
  "accountId": "20000000-0000-0000-0000-000000000001",
  "currency": "BRL",
  "cleared": 1250.0000,
  "pending": -100.0000,
  "projected": 1150.0000
}
```

## Categorias

### Criar categoria

`POST /api/v1/categories`

```json
{
  "name": "Supermercado",
  "kind": "EXPENSE"
}
```

`kind` aceita `INCOME` ou `EXPENSE`. Resposta `201 Created` com `Location`.

| Método | Endpoint | Função |
|---|---|---|
| `GET` | `/api/v1/categories?kind=EXPENSE&status=ACTIVE` | Lista com filtros opcionais. |
| `PATCH` | `/api/v1/categories/{id}` | Renomeia a categoria. |
| `DELETE` | `/api/v1/categories/{id}` | Arquiva e retorna `204`. |

Categorias arquivadas permanecem vinculadas ao histórico, mas não podem ser usadas em novos lançamentos.

## Lançamentos financeiros

### Criar receita ou despesa

`POST /api/v1/transactions`

Header obrigatório:

```text
Idempotency-Key: entry-2026-08-16-001
```

```json
{
  "accountId": "20000000-0000-0000-0000-000000000001",
  "categoryId": "30000000-0000-0000-0000-000000000001",
  "type": "EXPENSE",
  "amount": 149.90,
  "status": "CLEARED",
  "effectiveDate": "2026-08-16",
  "description": "Supermercado"
}
```

Criação direta aceita somente `INCOME` e `EXPENSE`. O status aceita `PENDING` ou `CLEARED`; cancelamento usa operação própria. A categoria deve ter tipo compatível.

### Consultar e manter lançamentos

| Método | Endpoint | Função |
|---|---|---|
| `GET` | `/api/v1/transactions/{id}` | Consulta um lançamento. |
| `GET` | `/api/v1/transactions` | Extrato paginado. |
| `PATCH` | `/api/v1/transactions/{id}` | Atualiza somente os campos enviados. |
| `POST` | `/api/v1/transactions/{id}/cancel` | Cancela preservando o histórico. |

Filtros opcionais do extrato:

- `accountId`;
- `categoryId`;
- `type`;
- `status`;
- `from` e `to` no formato `YYYY-MM-DD`;
- `page`, iniciando em zero;
- `size`, entre 1 e 100.

A ordenação é estável e decrescente por data efetiva, criação e UUID.

## Transferências

`POST /api/v1/transfers`

Exige `Idempotency-Key`.

```json
{
  "sourceAccountId": "20000000-0000-0000-0000-000000000001",
  "destinationAccountId": "20000000-0000-0000-0000-000000000002",
  "amount": 500.00,
  "effectiveDate": "2026-08-16",
  "description": "Reserva mensal"
}
```

As contas devem ser diferentes, ativas, do mesmo usuário e da mesma moeda. A resposta `201` contém o grupo, a perna `TRANSFER_OUT` e a perna `TRANSFER_IN`. As três linhas são persistidas na mesma transação de banco.

Retries concorrentes com a mesma chave e payload retornam o grupo persistido pela requisição vencedora. Se o payload for diferente, a API retorna `409 IDEMPOTENCY_CONFLICT`.

### Consultar transferência

`GET /api/v1/transfers/{id}`

Retorna o grupo e suas duas pernas somente quando pertencem ao usuário autenticado. Um UUID de outro usuário responde `404`.

### Cancelar transferência

`POST /api/v1/transfers/{id}/cancel`

```json
{
  "reason": "Conta de destino incorreta"
}
```

O motivo é obrigatório e limitado a 255 caracteres. A operação:

- muda o grupo para `CANCELED`;
- registra `cancelReason`, `canceledAt` e `updatedAt`;
- cancela `TRANSFER_OUT` e `TRANSFER_IN` com o mesmo instante;
- persiste grupo e pernas na mesma transação;
- é idempotente quando o grupo já está cancelado.

As pernas não podem ser editadas ou canceladas pelos endpoints de lançamentos. Toda mudança deve partir do grupo.

## Resumo financeiro

Todos os endpoints exigem `from` e `to` no formato `YYYY-MM-DD` e consideram lançamentos confirmados no período.

| Método | Endpoint | Resultado |
|---|---|---|
| `GET` | `/api/v1/summary?from=2026-08-01&to=2026-08-31` | Receita, despesa e resultado separados por moeda. |
| `GET` | `/api/v1/summary/by-category?from=...&to=...` | Total por categoria e moeda. |
| `GET` | `/api/v1/summary/timeline?from=...&to=...` | Evolução por mês e moeda. |

Transferências e saldo inicial afetam o patrimônio, mas não são contabilizados como receita ou despesa do período.

## Health check

`GET /actuator/health`

Não exige autenticação e é destinado a health/readiness checks.

## Erros

Os erros usam `Content-Type: application/problem+json`.

Exemplo de validação `400`:

```json
{
  "type": "https://cointrol.dev/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    "email": "must be a well-formed email address"
  }
}
```

| Status | Uso |
|---:|---|
| 400 | Payload/regra de validação inválida. |
| 401 | Credencial, access token ou refresh token inválido. |
| 403 | Usuário autenticado sem permissão. |
| 404 | Recurso inexistente. |
| 409 | E-mail duplicado ou violação de constraint de negócio. |
| 500 | Erro inesperado sem detalhes internos. |

Códigos financeiros relevantes:

| Código | Uso |
|---|---|
| `INVALID_MONEY_AMOUNT` | Valor inválido ou precisão não suportada. |
| `CATEGORY_TYPE_MISMATCH` | Categoria incompatível com receita/despesa. |
| `CURRENCY_MISMATCH` | Transferência entre moedas diferentes. |
| `ACCOUNT_ARCHIVED` | Tentativa de escrita em conta arquivada. |
| `IDEMPOTENCY_CONFLICT` | Chave já usada com payload diferente. |
| `CONCURRENT_MODIFICATION` | Recurso alterado concorrentemente. |
| `CANCEL_REASON_REQUIRED` | Cancelamento de transferência sem motivo. |
| `TRANSFER_ENTRY_IMMUTABLE` | Tentativa de alterar uma perna isoladamente. |

## Configuração do Swagger

Swagger é habilitado no perfil local:

```text
http://localhost:8080/swagger-ui.html
```

Em outros ambientes, depende de `SWAGGER_ENABLED=true` e deve permanecer desabilitado em produção salvo decisão explícita.
