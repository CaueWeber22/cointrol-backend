# Documentação do Cointrol

Este diretório concentra as decisões técnicas, instruções operacionais e evolução planejada do projeto.

## Documentos

- [IMPLEMENTACAO_ESTABILIZACAO.md](IMPLEMENTACAO_ESTABILIZACAO.md): alterações realizadas, riscos corrigidos, testes e pendências conhecidas.
- [IMPLEMENTACAO_MVP_FINANCEIRO.md](IMPLEMENTACAO_MVP_FINANCEIRO.md): features financeiras entregues, regras, validação e limites conhecidos.
- [GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md](GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md): sequência prática para implementar contas, categorias, lançamentos, saldo, transferências e resumo financeiro.
- [ARQUITETURA.md](ARQUITETURA.md): limites hexagonais, componentes e fluxos principais.
- [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md): schema PostgreSQL, migrations Flyway e operação local.
- [API.md](API.md): endpoints, payloads, autenticação e formato de erros.
- [SEGURANCA.md](SEGURANCA.md): rate limiting, bloqueio de login, auditoria, retenção e rotação segura de JWT.
- [CURLS_ENDPOINTS.md](CURLS_ENDPOINTS.md): exemplos `curl` prontos para cadastro, autenticação e operações financeiras.
- [postman/Cointrol.postman_collection.json](postman/Cointrol.postman_collection.json): collection Postman v2.1 para importar e testar a API local organizada por pastas.
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
