# Documentação do Cointrol

Este diretório concentra as decisões técnicas, instruções operacionais e evolução planejada do projeto.

## Documentos

- [IMPLEMENTACAO_ESTABILIZACAO.md](IMPLEMENTACAO_ESTABILIZACAO.md): alterações realizadas, riscos corrigidos, testes e pendências conhecidas.
- [IMPLEMENTACAO_MVP_FINANCEIRO.md](IMPLEMENTACAO_MVP_FINANCEIRO.md): features financeiras entregues, regras, validação e limites conhecidos.
- [GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md](GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md): sequência prática para implementar contas, categorias, lançamentos, saldo, transferências e resumo financeiro.
- [ARQUITETURA.md](ARQUITETURA.md): limites hexagonais, componentes e fluxos principais.
- [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md): schema PostgreSQL, migrations Flyway e operação local.
- [API.md](API.md): endpoints, payloads, autenticação e formato de erros.
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
└── V8__create_transfer_groups.sql
```

Eles não são duplicados em `/docs` para evitar duas fontes de verdade. O funcionamento e a política de evolução estão documentados em [BANCO_DE_DADOS.md](BANCO_DE_DADOS.md).
