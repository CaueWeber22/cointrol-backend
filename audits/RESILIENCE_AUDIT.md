# Auditoria de resiliência e tolerância a falhas

> **Relatório histórico.** Revisão documental em 2026-09-27, confrontada com o commit 1dce796. A data abaixo é a registrada no documento original; o SHA auditado originalmente não foi informado. Notas de risco, números de linha e resultados antigos não devem ser tratados como validação atual de produção.

## Atualização de contexto — 2026-09-27

- RES-01 parcialmente superado: application-prod.yml define timeouts de conexão/validação Hikari; não foi encontrado timeout explícito de consulta JPA nesse perfil.
- RES-02 parcialmente superado: server.shutdown=graceful e timeout-per-shutdown-phase=20s estão configurados no perfil prod. Drain/readiness durante rollout não foram testados.
- RES-05 parcialmente superado: o perfil prod limita Hikari a 5 conexões e Tomcat a 50 threads por padrão. O orçamento conjunto com réplicas e PostgreSQL continua sem teste de carga.
- Sessões CSRF agora residem na instância. Reinícios exigem novo GET /auth/csrf; múltiplas réplicas precisam de afinidade ou armazenamento compartilhado. Isso não revoga refresh tokens persistidos.

Consulte [o índice de auditorias](README.md) para escopo, limitações e validação recente.

## Relatório original

Data: 2026-09-26
Escopo: falhas de banco, saturação, deploy, jobs e degradação.

## Nota geral

Resiliência atual: **4/10**.

O sistema preserva atomicidade e idempotência nos fluxos financeiros principais, mas não define limites de tempo/recursos, shutdown gracioso, estratégia de recuperação ou telemetria suficiente. Como não há integrações HTTP externas, circuit breaker não é necessário hoje; retry cego de escrita financeira seria perigoso e não é recomendado.

## Achados

### RES-01 — Ausência de timeouts de consulta e orçamento de requisição

- Importância: **10/10**
- Evidência: `application.yml:1-76` não configura timeout Hikari/JPA nem limites de servidor. Resumos materializam o intervalo completo (`FinanceSummaryUtil.java:22-33`) e saldo percorre todo o histórico (`FinancePersistenceAdapter.java:143-151`).
- Falha esperada: consulta longa ocupa conexão e thread; chamadas concorrentes saturam o pool e aumentam latência em cascata.
- Correção: timeouts de conexão/consulta, limite máximo do intervalo de resumo, agregação SQL, limite de corpo no edge e teste de carga com volume representativo.

### RES-02 — Deploy pode interromper operações em andamento

- Importância: **9/10**
- Evidência: não existe `server.shutdown=graceful`, timeout de shutdown ou manifesto com readiness/drain/preStop.
- Falha esperada: restart/rollout encerra requests durante criação/cancelamento, obrigando clientes a reconciliar respostas ambíguas.
- Correção: shutdown gracioso, remoção da readiness antes de SIGTERM, janela de drain superior ao timeout máximo e smoke test durante rolling update. Idempotency keys já reduzem o risco de repetição em criações.

### RES-03 — Não há prova de recuperação do banco

- Importância: **10/10**
- Evidência: migrations existem, mas não há política de backup/PITR, RPO/RTO ou teste de restore no repositório.
- Falha esperada: erro humano, corrupção ou indisponibilidade regional pode causar perda prolongada de dados financeiros.
- Correção: backup automático, PITR, cópia protegida, restore periódico em ambiente isolado e runbook de failover/rollback. Medir tempo real do restore.

### RES-04 — Health/telemetria não sustentam detecção e diagnóstico

- Importância: **9/10**
- Evidência: apenas Actuator `health,info` (`application.yml:27-35`); sem métricas exportadas, tracing, logs estruturados ou correlation ID. Probes específicos não são públicos em `SecurityConfig.java:59-64`.
- Falha esperada: saturação, degradação e erros intermitentes são detectados pelo usuário antes da operação.
- Correção: readiness/liveness funcionais, métricas, alertas e logs correlacionados; nunca reiniciar instância somente porque o banco está momentaneamente lento se isso amplificar o incidente.

### RES-05 — Pool e threads não formam bulkheads explícitos

- Importância: **8/10**
- Evidência: Hikari e Tomcat usam defaults; não há sizing por réplica ou limite total contra `max_connections` do PostgreSQL.
- Falha esperada: muitas réplicas ou requests lentos excedem conexões e provocam fila global.
- Correção: definir orçamento `réplicas × pool máximo + jobs + manutenção < max_connections`; limitar threads/conexões HTTP; alertar por tempo de espera e conexões ativas. Considerar pool separado somente se jobs realmente competirem com tráfego.

### RES-06 — Jobs de retenção não são seguros para múltiplas réplicas e não limpam refresh tokens

- Importância: **8/10**
- Evidência: `ApplicationConfig.java:96` habilita scheduling em toda instância; `SecurityDataRetentionJob.java:38-49` executa deletes amplos em uma transação e não coordena réplicas. `RefreshTokenAdapters.java:35-56` cria linha em login/rotação, mas não existe expurgo.
- Falha esperada: todas as réplicas rodam o mesmo cleanup às 03:15, competem por locks e geram picos; `refresh_tokens` cresce indefinidamente.
- Correção: executar retenção como CronJob único ou usar lock distribuído; deletar em lotes; adicionar retenção de tokens expirados/revogados; monitorar duração, linhas removidas e falhas.

### RES-07 — Rate limiter local não degrada de forma previsível

- Importância: **8/10**
- Evidência: estado por JVM e varredura síncrona (`FixedWindowRateLimiter.java:12-14,55-60`); capacidade cheia nega toda chave nova (`30-35`).
- Falha esperada: picos de cardinalidade causam latência e bloqueio de clientes novos; redistribuir tráfego entre réplicas altera cotas.
- Correção: rate limit no edge/Redis, política explícita para falha do backend de limite e métricas separando quota excedida de falha interna.

### RES-08 — Retry deve permanecer seletivo e idempotente

- Importância: **6/10**
- Evidência positiva: lançamentos e transferências usam idempotency key e constraints; refresh usa lock pessimista; operações compostas usam transação. Não há retry genérico.
- Recomendação: não adicionar retry automático a transações financeiras inteiras. Retry somente para falhas transitórias classificadas, com poucas tentativas, jitter/backoff e idempotência comprovada. Cliente pode repetir criações com a mesma chave.

## Avaliação dos padrões

| Padrão | Estado |
|---|---|
| Timeouts | Fail |
| Retry com backoff | N/A/parcial; não há integração externa e escrita financeira não deve ter retry cego. |
| Circuit breaker | N/A hoje; apenas banco local ao serviço. |
| Bulkhead | Fail parcial; pools existem por default, sem orçamento explícito. |
| Graceful degradation | Fail; não há fallback/cap de custo para endpoints analíticos. |
| Idempotência | Pass nos fluxos de criação financeira. |
| Atomicidade | Pass em transferência e saldo inicial. |
| Recuperação/rollback | Unable to verify. |

## Validação

`mvnw --batch-mode verify`: **55 testes passaram, 0 falharam, 1 foi ignorado**. `DatabaseMigrationTest` foi ignorado porque Docker não estava disponível; isso precisa ser um gate obrigatório no CI de release.
