# Segurança

Este documento descreve os controles de autenticação, contenção de abuso, auditoria e gestão de chaves do Cointrol.

## Controles implementados

- access tokens JWT HMAC-SHA-256 com `kid`, emissor, audiência, `jti`, emissão e expiração;
- refresh tokens aleatórios de 512 bits, persistidos somente como SHA-256 e rotacionados a cada uso;
- suporte a chave JWT ativa e chaves anteriores durante uma rotação;
- bloqueio temporário por identificador depois de falhas consecutivas de login;
- rate limiting por IP para cadastro, login, refresh e demais endpoints da API;
- auditoria persistida de eventos de autenticação e contenção;
- retenção automática de tentativas e eventos de auditoria;
- respostas uniformes para credenciais inválidas, reduzindo enumeração de usuários.

## Proteção contra tentativas de login

O e-mail é normalizado e convertido em SHA-256 antes de ser usado no controle de tentativas. A tabela `access.login_attempts` não armazena o e-mail.

Configuração padrão:

| Variável | Padrão | Função |
|---|---:|---|
| `LOGIN_MAXIMUM_ATTEMPTS` | `5` | Falhas permitidas dentro da janela. |
| `LOGIN_ATTEMPT_WINDOW_SECONDS` | `900` | Janela de contagem, em segundos. |
| `LOGIN_LOCK_DURATION_SECONDS` | `900` | Duração do bloqueio temporário. |
| `LOGIN_ATTEMPT_RETENTION_DAYS` | `30` | Retenção de contadores sem atividade. |

O incremento é um `UPSERT` atômico no PostgreSQL. Uma autenticação bem-sucedida remove o contador. Durante o bloqueio, a API retorna `429 LOGIN_TEMPORARILY_BLOCKED` e o header `Retry-After`.

## Rate limiting

O filtro utiliza janelas fixas e limita o número de chaves mantidas em memória para evitar crescimento ilimitado.

| Escopo | Padrão |
|---|---:|
| Login | 10 requisições por minuto/IP |
| Refresh | 30 requisições por minuto/IP |
| Cadastro | 5 requisições por hora/IP |
| API geral | 300 requisições por minuto/IP |

As variáveis seguem os prefixos `RATE_LIMIT_LOGIN_*`, `RATE_LIMIT_REFRESH_*`, `RATE_LIMIT_REGISTRATION_*` e `RATE_LIMIT_API_*`. `RATE_LIMIT_ENABLED=false` desativa o filtro, o que deve ser reservado a testes controlados.

Quando o limite é excedido, a API retorna `429 RATE_LIMIT_EXCEEDED`, `Retry-After` e `retryAfterSeconds` no `ProblemDetail`.

O contador é local à instância. Em um ambiente com múltiplas réplicas, o próximo passo é mover esse estado para Redis ou aplicar o limite no API Gateway. O bloqueio de login continua compartilhado porque é persistido no PostgreSQL.

O endereço usado é `request.getRemoteAddr()`. Em produção atrás de proxy, configure encaminhamento apenas em uma cadeia de proxies confiável que remova headers recebidos do cliente; não habilite confiança irrestrita em `X-Forwarded-For`.

## Auditoria

Eventos gravados em `access.security_audit_events`:

- `LOGIN_SUCCESS`;
- `LOGIN_FAILURE`;
- `LOGIN_BLOCKED`;
- `TOKEN_REFRESH_SUCCESS`;
- `TOKEN_REFRESH_FAILURE`;
- `LOGOUT`;
- `RATE_LIMIT_EXCEEDED`.

Cada registro pode conter usuário, hash do identificador, IP, user-agent e instante. Senhas, JWTs, refresh tokens e seus valores brutos nunca são auditados.

Consulta operacional:

```sql
SELECT event_type, user_id, client_ip, occurred_at
FROM access.security_audit_events
WHERE occurred_at >= NOW() - INTERVAL '24 hours'
ORDER BY occurred_at DESC;
```

Por padrão, a retenção é de 365 dias e a limpeza acontece diariamente às 03:15 UTC. Configure por `SECURITY_AUDIT_RETENTION_DAYS` e `SECURITY_AUDIT_CLEANUP_CRON`, respeitando os requisitos legais do ambiente.

## Gestão e rotação de JWT

Variáveis principais:

| Variável | Função |
|---|---|
| `JWT_SECRET` | Segredo da chave ativa, entre 32 e 512 bytes. Obrigatório fora do perfil local. |
| `JWT_ACTIVE_KEY_ID` | Identificador `kid` da chave ativa. |
| `JWT_PREVIOUS_KEYS` | Chaves antigas no formato `kid:secret,kid:secret`. |
| `JWT_ISSUER` | Emissor exigido na validação. |
| `JWT_AUDIENCE` | Audiência exigida na validação. |
| `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES` | Validade entre 1 e 60 minutos. |
| `JWT_REFRESH_TOKEN_EXPIRATION_DAYS` | Validade entre 1 e 90 dias. |

Gere um segredo aleatório no PowerShell:

```powershell
$jwtBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
```

Procedimento de rotação sem invalidar imediatamente tokens ativos:

1. Mova o `JWT_ACTIVE_KEY_ID` e o `JWT_SECRET` atuais para `JWT_PREVIOUS_KEYS`.
2. Configure um novo identificador e um novo segredo aleatório como ativos.
3. Reinicie todas as instâncias com a mesma configuração.
4. Aguarde pelo menos a validade máxima do access token.
5. Remova a chave anterior de `JWT_PREVIOUS_KEYS`.

Exemplo conceitual:

```text
JWT_ACTIVE_KEY_ID=2026-09
JWT_SECRET=<novo-segredo>
JWT_PREVIOUS_KEYS=2026-08:<segredo-anterior>
```

Em produção, injete esses valores por Secret Manager, cofre ou mecanismo de secrets da plataforma. Não versione segredos, não os grave em logs e não os persista como variável permanente de usuário na estação.

## Resposta a incidentes

Em caso de suspeita de vazamento da chave ativa:

1. substitua imediatamente `JWT_SECRET` e `JWT_ACTIVE_KEY_ID`;
2. não mantenha a chave comprometida em `JWT_PREVIOUS_KEYS`;
3. revogue todos os refresh tokens ativos no banco;
4. revise `security_audit_events` e logs da infraestrutura;
5. preserve evidências conforme a política da organização.
