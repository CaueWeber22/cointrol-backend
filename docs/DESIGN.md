# Design do backend

Este documento registra decisões de design para API, domínio, segurança e persistência. Ele complementa [BUSINESS_RULES.md](BUSINESS_RULES.md), [API.md](API.md), [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md) e [SEGURANCA.md](SEGURANCA.md).

## Intenção do produto

Cointrol é uma API de controle financeiro pessoal. O backend deve favorecer correção, rastreabilidade, isolamento por usuário, idempotência em operações sensíveis e respostas previsíveis para o frontend.

## Design de API

- Use `/api/v1` como base dos endpoints versionados.
- Mantenha endpoints orientados a recursos: `accounts`, `categories`, `transactions`, `transfers`, `summary`, `auth` e `users`.
- O usuário proprietário vem do token; não aceite `userId` manipulável no payload financeiro.
- Use DTOs explícitos para requests e responses.
- Preserve compatibilidade de contrato sempre que possível.
- Para erros, mantenha `application/problem+json`, `code`, `status`, `title`, `detail` e `fieldErrors` quando houver validação de campo.
- Não exponha stack trace, causa interna, existência de usuários em falha de login, tokens ou hashes.

## Design de domínio

- Modele mudanças financeiras como comandos e casos de uso.
- Trate dinheiro como valor positivo com sinal derivado do tipo do lançamento.
- Não crie saldo materializado sem uma decisão explícita de arquitetura.
- Use cancelamento, arquivamento e histórico em vez de deleção física quando a informação tiver valor financeiro ou auditável.
- Transferência é uma operação composta, não dois lançamentos independentes editáveis.
- Idempotência faz parte do comportamento de criação de lançamentos e transferências.

## Design de segurança

- Todo recurso financeiro é propriedade de um usuário.
- A autenticação web usa cookies HttpOnly, Secure, SameSite=None e host-only. Tokens de autenticação não são expostos no JSON.
- CSRF usa token sincronizado em sessão no servidor; a autenticação JWT permanece stateless. Login/refresh rotacionam a sessão CSRF. Veja [FRONTEND_AUTH_COOKIES.md](FRONTEND_AUTH_COOKIES.md).
- CORS permite origens explícitas com credenciais. Cookies cross-site podem ser bloqueados pelo navegador; proxy same-origin ou domínios same-site são alternativas de infraestrutura.
- Falhas de autenticação devem ser uniformes o suficiente para reduzir enumeração.
- Refresh token é opaco para o cliente, persistido por hash, rotacionado e revogável.
- Access token JWT deve manter emissor, audiência, expiração curta, `kid` e validação de chave.
- Rate limiting, bloqueio temporário de login e auditoria são requisitos de produto.
- Logs e auditoria nunca devem armazenar senha, JWT, refresh token bruto ou segredo.

## Design de persistência

- Flyway é a fonte de verdade do schema.
- JPA entities representam persistência, não regra de negócio.
- Constraints de banco devem reforçar regras importantes: propriedade, unicidade ativa, idempotência, FKs compostas, status válido e integridade de transferência.
- Alterações destrutivas exigem plano de migração em etapas, backup e rollback.
- Use transações no adapter quando a porta exigir atomicidade externa.

## Design de evolução

- Adicione feature por fluxo completo: API, porta de entrada, caso de uso, porta de saída se necessário, adapter, migration e testes.
- Prefira nomes que revelem regra de negócio, não detalhe técnico.
- Não misture refactor amplo com mudança de comportamento sem necessidade.
- Atualize documentação quando alterar regra, endpoint, erro, migration ou segurança.
