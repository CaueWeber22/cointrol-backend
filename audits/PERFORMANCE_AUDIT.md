# Auditoria de performance e complexidade

> **Relatório histórico.** Revisão documental em 2026-09-27, confrontada com o commit 1dce796. A data abaixo é a registrada no documento original; o SHA auditado originalmente não foi informado. Notas de risco, números de linha e resultados antigos não devem ser tratados como validação atual de produção.

## Atualização de contexto — 2026-09-27

- O perfil prod agora configura pools/threads e usa até 10.000 chaves no rate limiter por padrão; o padrão geral permanece 100.000. A varredura síncrona descrita em PERF-04 permanece no código.
- A migração para cookies mantém a consulta de usuário do filtro JWT nos endpoints protegidos. Endpoints /auth usam suas próprias credenciais e são excluídos desse filtro.
- Contagens de linhas, notas de complexidade e resultados de testes abaixo pertencem ao levantamento original; não são métricas recalculadas do checkout atual.
- Não foram realizados benchmark, teste de carga ou EXPLAIN ANALYZE nesta revisão documental.

Consulte [o índice de auditorias](README.md) para escopo, limitações e validação recente.

## Relatório original

Data: 2026-09-06
Escopo: backend completo, com foco em HTTP, autenticação, persistência e cálculos financeiros.

## Resumo

Risco de performance: **7,0/10**.

Os fluxos CRUD são pequenos e a listagem de lançamentos limita a página a 100 itens. Os gargalos estão nos cálculos derivados e no caminho executado em toda requisição autenticada. Saldos e resumos têm custo linear no histórico e alocam entidades completas; cada endpoint autenticado ainda realiza duas buscas de usuário. Não houve benchmark nem `EXPLAIN ANALYZE`, pois o PostgreSQL/Testcontainers não estava disponível.

## Achados priorizados

### PERF-01 — Saldos e resumos materializam todo o histórico na JVM

- Importância: **10/10**
- Evidência: `FinanceSummaryUtil.java:29-33` executa três leituras e materializa contas, categorias e lançamentos; `FinancePersistenceAdapter.java:149-151` retorna todas as entidades do intervalo; `SummarizeUsecase.java:30-43`, `SummarizeTimelineUsecase.java:30-44` e `SummarizeByCategoryUsecase.java:32-53` agregam em memória. Para saldo, `GetAccountBalanceUsecase.java:25-40` percorre todos os lançamentos da conta após `FinancePersistenceAdapter.java:143-145` carregá-los e ordená-los.
- Custo: `O(E + A + C)` de transferência/alocação por resumo e `O(E_conta)` por saldo. O custo cresce sem limite e repete trabalho idêntico entre chamadas.
- Correção: adicionar contratos de saída para agregados e implementar consultas `SUM(CASE ...) GROUP BY currency/category/month` e saldo por conta no adapter PostgreSQL. Retornar projeções pequenas, mantendo SQL/JPA fora do núcleo.
- Verificação: comparar `EXPLAIN (ANALYZE, BUFFERS)` e heap/latência p95 com 10 mil, 100 mil e 1 milhão de lançamentos por usuário.

### PERF-02 — Duas consultas de usuário em toda requisição autenticada

- Importância: **9/10**
- Evidência: `JwtAuthenticationFilter.java:55-71` valida o token e chama `loadUserById`; `UserJPARepository.java:18-20` carrega também os roles por `EntityGraph`. Depois, `CurrentUserIdProvider.java:18-23` consulta novamente pelo e-mail para recuperar o UUID; praticamente todos os controllers financeiros chamam esse provider. `/users/me` também repete a busca diretamente (`UserController.java:51-53`).
- Custo: duas idas ao banco e dois joins de roles antes da consulta de negócio, aumentando latência e pressão no pool em todos os endpoints protegidos.
- Correção: definir um principal autenticado imutável com `UUID userId`, e-mail e authorities; o filtro o popula uma vez e controllers resolvem o UUID diretamente do principal. Se a consulta por request for requisito para revogação imediata de usuário, manter somente a primeira.
- Verificação: teste de integração com contador SQL deve comprovar uma única leitura de usuário por requisição autenticada.

### PERF-03 — Refresh tokens revogados/expirados crescem indefinidamente

- Importância: **8/10**
- Evidência: cada login insere uma linha (`AuthService.java:110-115`; `RefreshTokenAdapters.java:35-38`) e cada refresh revoga a antiga e insere outra (`RefreshTokenAdapters.java:43-56`). `SecurityDataRetentionJob.java:40-49` remove somente tentativas de login e auditoria. Não existe método de exclusão no `RefreshTokenJPARepository.java:15-24`. A tabela mantém três índices adicionais (`V1__create_access_schema.sql:42`; `V3__create_access_indexes.sql:1-9`).
- Custo: crescimento permanente de tabela/índices, bloat após futuras exclusões e aumento de manutenção/backup. Usuários que renovam com frequência geram uma linha a cada rotação.
- Correção: criar política explícita e job em lotes para apagar tokens expirados e revogados após uma margem de auditoria, por exemplo por `expires_at < cutoff`. Executar lotes pequenos e monitorar autovacuum.
- Verificação: teste de retenção e métrica de linhas ativas/revogadas/expiradas.

### PERF-04 — Limpeza do rate limiter varre até 100 mil entradas na thread da requisição

- Importância: **7/10**
- Evidência: a cada 512 requisições, `FixedWindowRateLimiter.java:55-60` executa `removeIf` sobre todo o `ConcurrentHashMap`; o padrão aceita 100.000 chaves (`application.yml:62-64`). Quando cheio, chaves novas são rejeitadas (`FixedWindowRateLimiter.java:30-35`).
- Custo: picos periódicos `O(K)` no caminho síncrono, contenção no mapa e cauda de latência irregular.
- Correção: usar cache com TTL/evicção amortizada (por exemplo Caffeine) ou mover o controle para gateway/Redis. Não realizar varredura completa na thread da requisição.
- Verificação: JMH ou teste de carga concorrente medindo p50/p95/p99 ao cruzar múltiplos de 512 aquisições.

### PERF-05 — Paginação por offset, count automático e filtros com `OR` opcional

- Importância: **6/10**
- Evidência: `FinancePersistenceAdapter.java:126-139` usa `PageRequest` e solicita `Page`, o que normalmente executa consulta de conteúdo e `count`. `FinancialEntryJPARepository.java:36-55` usa seis padrões `(:param is null or coluna = :param)`. O tamanho é limitado, mas o número da página não (`ListEntriesUsecase.java:21-29`).
- Custo: páginas profundas exigem descartar muitas linhas; o count percorre o conjunto filtrado; predicados opcionais podem produzir planos menos eficientes. O impacto do `OR` depende do plano efetivo do PostgreSQL e precisa ser confirmado.
- Correção: para navegação sequencial, usar paginação por cursor com `(effective_date, created_at, id)`; retornar `Slice` quando total exato não for necessário; gerar predicados apenas para filtros presentes. Limitar profundidade ou custo do offset.
- Verificação: `EXPLAIN (ANALYZE, BUFFERS)` para filtros comuns, sem filtros e páginas profundas.

### PERF-06 — Auditoria abre transação síncrona separada em autenticação

- Importância: **5/10**
- Evidência: `SecurityAuditPersistenceAdapter.java:17-28` usa `REQUIRES_NEW`; `AuthService.java:85-118` grava auditoria em bloqueio, falha e sucesso, e `AuthService.java:124-166` faz o mesmo em refresh/logout.
- Custo: uma conexão/transação adicional em cada evento de autenticação, além das consultas e atualizações do fluxo. Em indisponibilidade do banco de auditoria, a exceção também interfere no resultado do login/refresh/logout.
- Correção: manter semântica durável, mas considerar outbox na transação principal ou fila limitada com fallback e métricas. Não perder eventos silenciosamente; definir explicitamente comportamento fail-open/fail-closed.
- Verificação: teste de carga de login e teste de falha do repositório de auditoria.

### QUAL-01 — Composition root acima do limiar de tamanho, sem complexidade algorítmica alta

- Importância: **3/10**
- Evidência: `ApplicationConfig.java` tem 349 linhas, acima do limiar de 300 da auditoria. `FinancePersistenceAdapter.java` tem 283 e `AuthService.java` 244. Não foram identificados métodos produtivos acima de 50 linhas nem controle de fluxo com complexidade ciclomática claramente maior que 10; o maior trecho cognitivo é o UPSERT em `LoginAttemptPersistenceAdapter.java:20-50`.
- Impacto: o arquivo de configuração tem alto acoplamento e diff amplo, embora isso seja parcialmente esperado em um composition root. O adapter financeiro concentra quatro agregados/repositórios e tende a crescer.
- Correção: dividir `ApplicationConfig` por contexto (`UserBeans`, `AuthBeans`, `FinanceBeans`) e, quando houver nova funcionalidade, separar adapters de conta, categoria, lançamento e transferência sem duplicar mapeamentos.

## Métricas qualitativas

| Área | Avaliação |
|---|---|
| Complexidade ciclomática | Baixa a moderada; nenhum método claramente acima de 10. |
| Complexidade cognitiva | Moderada em autenticação e no UPSERT de tentativas; baixa nos use cases financeiros. |
| Métodos > 50 linhas | Nenhum método produtivo identificado. |
| Arquivos > 300 linhas | `ApplicationConfig.java` (349). |
| Classes > 500 linhas | Nenhuma. |
| Acoplamento | Alto no composition root (esperado) e moderado/alto em `FinancePersistenceAdapter`. |
| Coesão | Boa no núcleo; `FinancePersistenceAdapter` agrega responsabilidades de quatro recursos. |

## Pontos positivos

- `ListEntriesUsecase.java:23-24` limita páginas a 100 itens.
- `application.yml:14-20` desativa Open Session in View e SQL logging.
- Índices financeiros começam por `user_id`, consistente com isolamento e consultas predominantes (`V7__create_financial_entries.sql:46-58`).
- Transferências são persistidas em transação única (`FinancePersistenceAdapter.java:168-206`).
- A consulta de tentativas de login usa UPSERT atômico, evitando read-modify-write concorrente.

## Validação e lacunas

- `mvnw verify`: **55 testes passaram, 0 falharam, 1 ignorado**.
- `DatabaseMigrationTest` foi ignorado por indisponibilidade do Docker.
- Sem PostgreSQL ativo não foi possível medir planos, buffers, contenção, pool ou latência real.
- Não há benchmarks/testes de carga; as classificações de custo são derivadas das estruturas de dados e consultas observadas.
