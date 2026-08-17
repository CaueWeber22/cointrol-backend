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
- transferir valores atomicamente entre contas da mesma moeda;
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

Os scripts executáveis continuam em `src/main/resources/db/migration` e estão descritos em `BANCO_DE_DADOS.md`.

## Validação realizada

```text
Tests run: 36, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Instruction coverage: 51.1%
Branch coverage: 49.5%
```

O teste ignorado é o de migrations com PostgreSQL/Testcontainers, pois o Docker daemon estava desligado no ambiente local. O teste foi atualizado para aplicar as oito migrations, validar os dois schemas, contar tabelas e verificar FKs. Ele deve executar automaticamente no CI e quando o Docker estiver ativo.

## Limites intencionais deste MVP

- Transferências são criadas, mas ainda não possuem endpoint próprio de edição/cancelamento.
- Não há conversão cambial.
- Não há categorias globais pré-carregadas.
- Não há recorrências, parcelas, orçamentos, metas, cartões ou importação.
- Resumos são calculados sobre lançamentos carregados no período; otimizações SQL podem ser introduzidas quando houver volume medido.
- A proteção de idempotência concorrente termina em `409` quando duas primeiras requisições chegam exatamente juntas; um retry posterior recupera o resultado persistido.

## Próximas melhorias recomendadas

1. Executar o teste Testcontainers e um smoke test completo com Docker ativo.
2. Adicionar cancelamento atômico de transferências.
3. Criar categorias padrão no onboarding.
4. Evoluir os resumos para projections SQL após medir volume e latência.
5. Adicionar orçamentos mensais por categoria.
6. Implementar recorrências e parcelamentos idempotentes.
7. Adicionar métricas, correlation ID e auditoria de alterações financeiras.
