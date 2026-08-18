# Implementação do MVP financeiro

**Data:** 16 de agosto de 2026

## Resultado

O Cointrol agora possui um primeiro núcleo financeiro funcional, construído como cortes verticais sobre a arquitetura hexagonal existente.

O usuário autenticado pode:

- criar, listar, consultar, renomear e arquivar contas;
- registrar saldo inicial como lançamento contábil;
- criar, listar, renomear e arquivar categorias;
- criar receitas e despesas idempotentes;
- consultar, filtrar, editar e cancelar lançamentos;
- consultar saldo confirmado, pendente e projetado;
- criar, consultar e cancelar transferências atomicamente entre contas da mesma moeda;
- consultar resumo por moeda, categoria e mês.

## Regras implementadas

### Propriedade

- O proprietário é obtido da autenticação; a API não aceita `userId` nos payloads.
- Casos de uso e queries recebem o `userId` explicitamente.
- Recursos de outro usuário são tratados como inexistentes.
- FKs compostas garantem no PostgreSQL que conta, categoria, transferência e lançamento pertencem ao mesmo usuário.

### Dinheiro e moeda

- Valores usam `BigDecimal` e `NUMERIC(19,4)`.
- Valores de entrada devem ser positivos e ter no máximo quatro casas decimais.
- Contas usam moeda ISO 4217 normalizada em maiúsculas.
- Transferências entre moedas diferentes são rejeitadas.
- Resumos nunca somam moedas diferentes.

### Razão e saldo

- Não existe coluna de saldo editável.
- Saldo inicial opcional cria `OPENING_BALANCE` na mesma transação da conta.
- Saldo confirmado considera apenas lançamentos `CLEARED`.
- Saldo pendente é apresentado separadamente.
- Lançamentos cancelados permanecem armazenados e deixam de compor os saldos.
- Transferências alteram os saldos, mas não inflam receitas e despesas nos resumos.

### Idempotência

- `POST /transactions` e `POST /transfers` exigem `Idempotency-Key`.
- A chave é única por usuário.
- Um retry com o mesmo payload devolve o resultado original.
- A mesma chave com outro payload retorna `409 IDEMPOTENCY_CONFLICT`.
- O PostgreSQL possui índices/constraints únicos como última barreira contra duplicidade.

### Concorrência e atomicidade

- Contas, categorias e lançamentos usam versão otimista.
- Conflitos de versão retornam `409 CONCURRENT_MODIFICATION`.
- A persistência de uma transferência grava grupo, débito e crédito na mesma transação.
- O cancelamento grava motivo/instante no grupo e cancela as duas pernas na mesma transação.
- Pernas de transferência não podem ser alteradas ou canceladas isoladamente.
- Uma corrida de criação idempotente recarrega e retorna a transferência vencedora após rollback.
- O saldo inicial e a conta também são persistidos atomicamente.

## Arquitetura

O núcleo financeiro está organizado em:

```text
application/core/domain/finance/FinanceModels.java
application/core/commands/finance/FinanceCommands.java
application/core/usecases/finance/FinanceService.java
application/ports/inbound/finance/FinanceInPort.java
application/ports/outbound/finance/FinanceOutPort.java
adapters/inbound/controllers/*Controller.java
adapters/inbound/dto/finance/
adapters/outbound/FinancePersistenceAdapter.java
adapters/outbound/entities/finance/
adapters/outbound/persistence/finance/
```

`FinanceService` não importa Spring, JPA, Jackson ou classes de adapters. O wiring permanece em `ApplicationConfig` e continua protegido por ArchUnit.

## Migrations

Foram adicionadas:

| Versão | Entrega |
|---|---|
| V4 | Schema `finance`. |
| V5 | Contas, índices e unicidade de nome ativo. |
| V6 | Categorias e unicidade por tipo. |
| V7 | Lançamentos, idempotência e índices de extrato. |
| V8 | Grupos de transferência e vínculo das duas pernas. |
| V9 | Estado, motivo, instante e versão para cancelamento do grupo. |
| V10 | Alinhamento de `accounts.currency` com o tipo `VARCHAR(3)` esperado pelo Hibernate. |

Os scripts executáveis continuam em `src/main/resources/db/migration` e estão descritos em `BANCO_DE_DADOS.md`.

## Validação realizada

```text
Tests run: 43, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Instruction coverage: 58.4%
Branch coverage: 51.9%
```

O teste ignorado é o de migrations com PostgreSQL/Testcontainers, pois o Docker daemon estava desligado no ambiente local. O teste foi atualizado para aplicar as dez migrations, validar os dois schemas, contar tabelas, verificar FKs, conferir as colunas de cancelamento e validar o tipo da moeda. Ele deve executar automaticamente no CI e quando o Docker estiver ativo.

## Limites intencionais deste MVP

- Não há conversão cambial.
- Não há categorias globais pré-carregadas.
- Não há recorrências, parcelas, orçamentos, metas, cartões ou importação.
- Resumos são calculados sobre lançamentos carregados no período; otimizações SQL podem ser introduzidas quando houver volume medido.
- Transferências canceladas não podem ser reabertas; uma correção posterior exige nova transferência.

## Próximas melhorias recomendadas

1. Executar o teste Testcontainers e um smoke test completo com Docker ativo.
2. Criar categorias padrão no onboarding.
3. Evoluir os resumos para projections SQL após medir volume e latência.
4. Adicionar orçamentos mensais por categoria.
5. Implementar recorrências e parcelamentos idempotentes.
6. Adicionar métricas, correlation ID e auditoria de alterações financeiras.
