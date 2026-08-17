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

## Configuração do Swagger

Swagger é habilitado no perfil local:

```text
http://localhost:8080/swagger-ui.html
```

Em outros ambientes, depende de `SWAGGER_ENABLED=true` e deve permanecer desabilitado em produção salvo decisão explícita.
