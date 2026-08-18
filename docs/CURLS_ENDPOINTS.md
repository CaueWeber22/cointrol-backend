# Exemplos `curl` dos endpoints

Este documento reúne exemplos de chamadas para todos os endpoints disponíveis na API do Cointrol.

Os comandos usam sintaxe compatível com Bash, Git Bash, WSL, Linux e macOS. No Windows PowerShell, execute-os pelo Git Bash/WSL ou substitua `curl` por `curl.exe` e adapte as variáveis e quebras de linha.

## Preparação

Com a aplicação disponível em `http://localhost:8080`, configure as variáveis abaixo:

```bash
export API_URL="http://localhost:8080"
export ACCESS_TOKEN="cole-o-access-token-aqui"
export REFRESH_TOKEN="cole-o-refresh-token-aqui"

export ACCOUNT_ID="20000000-0000-0000-0000-000000000001"
export DESTINATION_ACCOUNT_ID="20000000-0000-0000-0000-000000000002"
export CATEGORY_ID="30000000-0000-0000-0000-000000000001"
export TRANSACTION_ID="40000000-0000-0000-0000-000000000001"
export TRANSFER_ID="50000000-0000-0000-0000-000000000001"
```

As variáveis de IDs devem ser atualizadas com os valores devolvidos pelas operações de criação.

## Health check

Não exige autenticação.

```bash
curl --request GET \
  --url "$API_URL/actuator/health"
```

## Usuários e autenticação

### Cadastrar usuário

```bash
curl --request POST \
  --url "$API_URL/api/v1/users" \
  --header "Content-Type: application/json" \
  --data '{
    "first_name": "Ada",
    "last_name": "Lovelace",
    "email": "ada@example.com",
    "phone": "+55 11 99999-9999",
    "gender": "female",
    "date_of_birth": "1990-01-01",
    "password": "Valid@123"
  }'
```

### Login

```bash
curl --request POST \
  --url "$API_URL/api/v1/auth/login" \
  --header "Content-Type: application/json" \
  --data '{
    "email": "ada@example.com",
    "password": "Valid@123"
  }'
```

Copie `accessToken` e `refreshToken` da resposta para `ACCESS_TOKEN` e `REFRESH_TOKEN`.

Com `jq`, isso pode ser automatizado:

```bash
TOKENS=$(curl --silent --request POST \
  --url "$API_URL/api/v1/auth/login" \
  --header "Content-Type: application/json" \
  --data '{"email":"ada@example.com","password":"Valid@123"}')

export ACCESS_TOKEN=$(echo "$TOKENS" | jq -r '.accessToken')
export REFRESH_TOKEN=$(echo "$TOKENS" | jq -r '.refreshToken')
```

### Renovar tokens

O refresh token usado é revogado e a resposta contém um novo par de tokens.

```bash
curl --request POST \
  --url "$API_URL/api/v1/auth/refresh" \
  --header "Content-Type: application/json" \
  --data "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### Logout

```bash
curl --request POST \
  --url "$API_URL/api/v1/auth/logout" \
  --header "Content-Type: application/json" \
  --data "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### Consultar perfil autenticado

```bash
curl --request GET \
  --url "$API_URL/api/v1/users/me" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

## Contas financeiras

Tipos aceitos: `CHECKING`, `SAVINGS`, `CASH` e `INVESTMENT`.

### Criar conta

```bash
curl --request POST \
  --url "$API_URL/api/v1/accounts" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Conta principal",
    "type": "CHECKING",
    "currency": "BRL",
    "openingBalance": 1500.00
  }'
```

`openingBalance` é opcional. Use o `id` retornado para atualizar `ACCOUNT_ID`.

### Listar contas

Sem filtro:

```bash
curl --request GET \
  --url "$API_URL/api/v1/accounts" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

Filtrando por `ACTIVE` ou `ARCHIVED`:

```bash
curl --request GET \
  --url "$API_URL/api/v1/accounts?status=ACTIVE" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Consultar conta

```bash
curl --request GET \
  --url "$API_URL/api/v1/accounts/$ACCOUNT_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Renomear conta

```bash
curl --request PATCH \
  --url "$API_URL/api/v1/accounts/$ACCOUNT_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"name":"Conta principal atualizada"}'
```

### Consultar saldo

```bash
curl --request GET \
  --url "$API_URL/api/v1/accounts/$ACCOUNT_ID/balance" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Arquivar conta

```bash
curl --request DELETE \
  --url "$API_URL/api/v1/accounts/$ACCOUNT_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

## Categorias

Tipos aceitos: `INCOME` e `EXPENSE`.

### Criar categoria

```bash
curl --request POST \
  --url "$API_URL/api/v1/categories" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Supermercado",
    "kind": "EXPENSE"
  }'
```

Use o `id` retornado para atualizar `CATEGORY_ID`.

### Listar categorias

Sem filtros:

```bash
curl --request GET \
  --url "$API_URL/api/v1/categories" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

Com filtros opcionais:

```bash
curl --request GET \
  --url "$API_URL/api/v1/categories?kind=EXPENSE&status=ACTIVE" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Renomear categoria

```bash
curl --request PATCH \
  --url "$API_URL/api/v1/categories/$CATEGORY_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"name":"Mercado e alimentação"}'
```

### Arquivar categoria

```bash
curl --request DELETE \
  --url "$API_URL/api/v1/categories/$CATEGORY_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

## Lançamentos financeiros

Criações exigem uma `Idempotency-Key` exclusiva. Reutilize a mesma chave somente para repetir exatamente a mesma requisição.

### Criar receita ou despesa

Tipos de criação aceitos: `INCOME` e `EXPENSE`. Status aceitos: `PENDING` e `CLEARED`.

```bash
curl --request POST \
  --url "$API_URL/api/v1/transactions" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --header "Idempotency-Key: entry-2026-08-16-001" \
  --data "{
    \"accountId\": \"$ACCOUNT_ID\",
    \"categoryId\": \"$CATEGORY_ID\",
    \"type\": \"EXPENSE\",
    \"amount\": 149.90,
    \"status\": \"CLEARED\",
    \"effectiveDate\": \"2026-08-16\",
    \"description\": \"Supermercado\"
  }"
```

Use o `id` retornado para atualizar `TRANSACTION_ID`.

### Consultar lançamento

```bash
curl --request GET \
  --url "$API_URL/api/v1/transactions/$TRANSACTION_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Listar extrato

Sem filtros, usando a paginação padrão:

```bash
curl --request GET \
  --url "$API_URL/api/v1/transactions" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

Com todos os filtros disponíveis:

```bash
curl --request GET \
  --url "$API_URL/api/v1/transactions?accountId=$ACCOUNT_ID&categoryId=$CATEGORY_ID&type=EXPENSE&status=CLEARED&from=2026-08-01&to=2026-08-31&page=0&size=20" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

`page` começa em zero e `size` deve ficar entre 1 e 100.

### Atualizar lançamento

Envie somente os campos que devem mudar. O tipo do lançamento não é alterável.

```bash
curl --request PATCH \
  --url "$API_URL/api/v1/transactions/$TRANSACTION_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "amount": 159.90,
    "status": "CLEARED",
    "effectiveDate": "2026-08-17",
    "description": "Supermercado atualizado"
  }'
```

Também podem ser enviados `accountId` e `categoryId`. Pernas de transferência não podem ser atualizadas por este endpoint.

### Cancelar lançamento

```bash
curl --request POST \
  --url "$API_URL/api/v1/transactions/$TRANSACTION_ID/cancel" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

Pernas de transferência devem ser canceladas pelo grupo da transferência.

## Transferências

### Criar transferência

As contas precisam pertencer ao mesmo usuário, estar ativas, ter a mesma moeda e ser diferentes.

```bash
curl --request POST \
  --url "$API_URL/api/v1/transfers" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --header "Idempotency-Key: transfer-2026-08-16-001" \
  --data "{
    \"sourceAccountId\": \"$ACCOUNT_ID\",
    \"destinationAccountId\": \"$DESTINATION_ACCOUNT_ID\",
    \"amount\": 500.00,
    \"effectiveDate\": \"2026-08-16\",
    \"description\": \"Reserva mensal\"
  }"
```

A resposta contém o grupo e as pernas `TRANSFER_OUT` e `TRANSFER_IN`. Use o `id` do grupo para atualizar `TRANSFER_ID`.

### Consultar transferência pelo grupo

```bash
curl --request GET \
  --url "$API_URL/api/v1/transfers/$TRANSFER_ID" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Cancelar transferência

O cancelamento registra o motivo e o instante e cancela as duas pernas atomicamente.

```bash
curl --request POST \
  --url "$API_URL/api/v1/transfers/$TRANSFER_ID/cancel" \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"reason":"Conta de destino incorreta"}'
```

Repetir o cancelamento de um grupo já cancelado devolve o estado persistido sem cancelar cada perna isoladamente.

## Resumo financeiro

As datas `from` e `to` são obrigatórias e usam o formato `YYYY-MM-DD`.

### Resumo por moeda

```bash
curl --request GET \
  --url "$API_URL/api/v1/summary?from=2026-08-01&to=2026-08-31" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Resumo por categoria

```bash
curl --request GET \
  --url "$API_URL/api/v1/summary/by-category?from=2026-08-01&to=2026-08-31" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

### Linha do tempo mensal

```bash
curl --request GET \
  --url "$API_URL/api/v1/summary/timeline?from=2026-01-01&to=2026-12-31" \
  --header "Authorization: Bearer $ACCESS_TOKEN"
```

## Observações de uso

- Endpoints protegidos exigem `Authorization: Bearer $ACCESS_TOKEN`.
- Payloads JSON exigem `Content-Type: application/json`.
- Datas usam o padrão ISO `YYYY-MM-DD`.
- Valores monetários precisam ser positivos e aceitam até quatro casas decimais.
- `Idempotency-Key` é obrigatória na criação de lançamentos e transferências.
- A mesma chave de idempotência com payload diferente retorna `409 IDEMPOTENCY_CONFLICT`.
- Erros são retornados como `application/problem+json`.

Para os contratos de resposta, regras e códigos de erro, consulte [API.md](API.md).
