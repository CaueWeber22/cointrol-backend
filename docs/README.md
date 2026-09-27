# Documentação do Cointrol

Este diretório concentra as decisões técnicas, instruções operacionais e evolução planejada do projeto.

## Documentos

- [IMPLEMENTACAO_ESTABILIZACAO.md](IMPLEMENTACAO_ESTABILIZACAO.md): alterações realizadas, riscos corrigidos, testes e pendências conhecidas.
- [IMPLEMENTACAO_MVP_FINANCEIRO.md](IMPLEMENTACAO_MVP_FINANCEIRO.md): features financeiras entregues, regras, validação e limites conhecidos.
- [GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md](GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md): sequência prática para implementar contas, categorias, lançamentos, saldo, transferências e resumo financeiro.
- [BUSINESS_RULES.md](BUSINESS_RULES.md): regras de negócio consolidadas para autenticação, usuários, recursos financeiros, saldos e resumos.
- [ARCHITECTURE.md](ARCHITECTURE.md): mapa curto da arquitetura do backend para agentes e futuras alterações.
- [DESIGN.md](DESIGN.md): decisões de design de API, domínio, segurança, persistência e evolução.
- [ARQUITETURA.md](ARQUITETURA.md): limites hexagonais, componentes e fluxos principais.
- [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md): schema PostgreSQL, migrations Flyway e operação local.
- [API.md](API.md): endpoints, payloads, autenticação e formato de erros.
- [FRONTEND_AUTH_COOKIES.md](FRONTEND_AUTH_COOKIES.md): contrato web de cookies, CSRF, CORS e integração Angular.
- [../audits/README.md](../audits/README.md): relatórios históricos de auditoria e correções de contexto verificadas.
- [SEGURANCA.md](SEGURANCA.md): rate limiting, bloqueio de login, auditoria, retenção e rotação segura de JWT.
- [CURLS_ENDPOINTS.md](CURLS_ENDPOINTS.md): exemplos `curl` prontos para cadastro, autenticação e operações financeiras.
- [AVALIACAO_TECNICA_E_ROADMAP.md](AVALIACAO_TECNICA_E_ROADMAP.md): auditoria original, backlog técnico e roadmap das features financeiras.

## Fonte executável dos scripts SQL

Os scripts aplicados pela aplicação ficam em:

```text
src/main/resources/db/migration/
├── V1__create_access_schema.sql
├── V2__seed_default_roles.sql
├── V3__create_access_indexes.sql
├── V4__create_finance_schema.sql
├── V5__create_accounts.sql
├── V6__create_categories.sql
├── V7__create_financial_entries.sql
├── V8__create_transfer_groups.sql
├── V9__add_transfer_cancellation.sql
├── V10__align_account_currency_type.sql
└── V11__add_security_controls.sql
```

Eles não são duplicados em `/docs` para evitar duas fontes de verdade. O funcionamento e a política de evolução estão documentados em [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md).
