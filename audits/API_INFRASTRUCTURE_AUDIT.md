# Auditoria de API e infraestrutura para produção

> **Relatório histórico.** Revisão documental em 2026-09-27, confrontada com o commit 1dce796. A data abaixo é a registrada no documento original; o SHA auditado originalmente não foi informado. Notas de risco, números de linha e resultados antigos não devem ser tratados como validação atual de produção.

## Atualização de contexto — 2026-09-27

- INFRA-01 parcialmente superado: há Dockerfile multi-stage Java 21 com usuário não-root e perfil prod. Não foi verificada publicação de imagem imutável, scan, rollout ou rollback.
- INFRA-02: pom.xml usa Spring Boot 4.1.1; o diagnóstico baseado em 3.4.2 não se aplica ao checkout atual. Não houve nova avaliação de suporte upstream ou SCA.
- INFRA-03 parcialmente superado: application-prod.yml configura Hikari, threads Tomcat, shutdown gracioso e janela de 20s. Limites de corpo, timeout de consulta e confiança em proxy continuam precisando de validação.
- CORS agora tem a origem web explícita como padrão fora do perfil local e rejeita curingas. Cookies/CSRF e suas limitações estão documentados em ../docs/FRONTEND_AUTH_COOKIES.md.
- A classificação no-go abaixo pertence ao relatório original; não é um parecer operacional atualizado nem evidência sobre a configuração efetiva do Render.

Consulte [o índice de auditorias](README.md) para escopo, limitações e validação recente.

## Relatório original

Data: 2026-09-26
Escopo: API Spring Boot, configuração, CI, banco e artefatos operacionais.
Premissa: deploy em container, atrás de proxy/load balancer HTTPS, com PostgreSQL gerenciado.

## Parecer

Risco atual para produção pública: **7,5/10 (alto)**.
Decisão recomendada: **no-go para produção pública; apto para staging após configurar segredos e PostgreSQL**.

A aplicação tem boa base de domínio, autenticação e integridade financeira, mas o repositório ainda descreve apenas execução local. Não há imagem da API, perfil de produção, configuração de proxy confiável, limites de requisição, telemetria exportável, processo de migration/rollback ou estratégia verificável de backup.

## Achados

### INFRA-01 — Não existe artefato ou procedimento reprodutível de deploy da API

- Severidade: **Alta**
- Evidência: não há `Dockerfile`, manifesto Kubernetes/Helm/Terraform nem workflow de entrega. `compose.yml:1-21` sobe apenas PostgreSQL, publica `5432` e usa senha local padrão.
- Impacto: runtime, usuário do processo, recursos, health check, rollout e rollback ficam implícitos e variam por operador.
- Correção: criar imagem multi-stage Java 21, executar como usuário não-root, fixar versão/digest da base, expor somente a porta HTTP da API e publicar imagem imutável com tag de release/SHA. Criar runbook de deploy, smoke test e rollback.
- Defesa em profundidade: filesystem read-only, `/tmp` separado, limites de CPU/memória, SBOM e scan da imagem.

### INFRA-02 — Spring Boot 3.4.2 está fora da linha open source suportada

- Severidade: **Alta**
- CWE: **CWE-1104**
- Evidência: `pom.xml:5-9` fixa `spring-boot-starter-parent` em `3.4.2`.
- Impacto: o BOM deixa framework, servidor e bibliotecas transitivas sem patches correntes. Em 2026-09, a última versão estável publicada é 4.1.1; 3.5.16 foi a última release OSS da geração 3.5, e a equipe Spring recomenda migrar para 4.0.x ou 4.1.x.
- Correção: atualizar para **Spring Boot 4.1.1 ou patch 4.1.x suportado mais recente disponível no dia do deploy**, atualizar `springdoc` e seguir as notas de migração. Se necessário, usar 3.5.16 apenas como etapa temporária de compatibilidade, não como destino operacional de longo prazo. Executar toda a suíte, smoke tests e teste integrado PostgreSQL após cada etapa.
- Defesa em profundidade: Dependabot/Renovate, SCA no CI e janela mensal de patches.

### INFRA-03 — Configuração de produção, limites e timeouts não estão definidos

- Severidade: **Alta**
- CWE: **CWE-400**
- Evidência: `application.yml:1-76` não define perfil `prod`, shutdown gracioso, timeout HTTP/JPA, limites de headers/corpo, sizing Hikari/Tomcat ou política de forwarded headers. `application-local.yml:1-18` contém defaults exclusivamente locais.
- Impacto: requisições lentas ou grandes podem ocupar threads/conexões; deploys encerram requisições em andamento; o comportamento muda conforme defaults de versão.
- Correção mínima em `application-prod.yml`:

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
  datasource:
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE:10}
      minimum-idle: ${DB_POOL_MIN_IDLE:2}
      connection-timeout: ${DB_CONNECTION_TIMEOUT_MS:3000}
      validation-timeout: ${DB_VALIDATION_TIMEOUT_MS:1000}
  jpa:
    properties:
      jakarta.persistence.query.timeout: ${DB_QUERY_TIMEOUT_MS:5000}
server:
  shutdown: graceful
  max-http-request-header-size: 16KB
  forward-headers-strategy: ${FORWARD_HEADERS_STRATEGY:none}
  tomcat:
    connection-timeout: 5s
```

O limite do corpo JSON deve existir no proxy/ingress (por exemplo 64 KiB para esta API) antes da desserialização. Sizing deve ser validado por teste de carga, não copiado cegamente.

### INFRA-04 — Rate limit depende da JVM e o IP real não está operacionalizado

- Severidade: **Alta**
- CWE: **CWE-400**
- Evidência: `RateLimitFilter.java:72-75,98-105` usa `request.getRemoteAddr()`. `FixedWindowRateLimiter.java:12-14` mantém estado em memória; `FixedWindowRateLimiter.java:30-35` rejeita qualquer chave nova quando a capacidade é alcançada; a limpeza completa roda na thread da requisição (`55-60`).
- Impacto: atrás de proxy, todos podem compartilhar a mesma chave se forwarded headers não forem processados; confiar em headers vindos da internet permite spoofing; múltiplas réplicas multiplicam a cota; esgotar o mapa nega novos clientes.
- Correção: aplicar rate limit no gateway/WAF ou Redis com TTL e atomicidade. Configurar forwarding apenas quando o balanceador remove headers do cliente e reinsere valores confiáveis. Manter limites distintos para login, cadastro, refresh e API.
- Beta de uma réplica: pode manter o limitador local temporariamente, desde que o proxy produza IP confiável, haja alerta de capacidade e a política fail-closed seja corrigida.

### INFRA-05 — Banco de produção não tem postura versionada de segurança e recuperação

- Severidade: **Alta**
- CWE: **CWE-319** (se transporte sem TLS)
- Evidência: `application.yml:4-13` recebe uma URL JDBC genérica e executa Flyway no startup. Não há configuração/versionamento de TLS PostgreSQL, roles separadas, backup, PITR ou restore. `compose.yml:10-11` publica o banco local na máquina.
- Impacto: um `PG_DB_URL` inadequado pode usar transporte sem validação; a credencial da aplicação precisa de DDL se Flyway continuar no startup; não há prova de recuperação de dados financeiros.
- Correção: PostgreSQL privado/gerenciado, criptografia em repouso, `sslmode=verify-full`, CA validada, backup automático/PITR e teste de restore. Separar role de migration da role runtime com privilégio mínimo; executar Flyway como job único antes do rollout.
- Defesa em profundidade: alertas de storage/conexões/replication lag, RPO/RTO documentados e restore ensaiado.

### INFRA-06 — Health probes e observabilidade são insuficientes para operar

- Severidade: **Média**
- Evidência: `application.yml:27-35` expõe apenas `health,info`; não há registry Prometheus/OpenTelemetry, log estruturado ou correlation ID. `SecurityConfig.java:62` libera apenas `/actuator/health` e `/actuator/info`, não as rotas `/actuator/health/liveness` e `/readiness` habilitadas pelos probes.
- Impacto: o orquestrador pode não distinguir processo vivo de instância pronta; erros, saturação de pool e aumento de latência não produzem alerta acionável.
- Correção: liberar somente health/liveness/readiness sem detalhes sensíveis, adicionar `micrometer-registry-prometheus` ou OTLP, logs JSON com request/correlation ID e dashboards/alertas para 5xx, p95/p99, pool JDBC, heap/GC, rate limit e falhas de login.
- Defesa em profundidade: SLO inicial (disponibilidade e latência) e alertas por burn rate.

### INFRA-07 — Proteção de login pode ser usada para bloquear uma vítima

- Severidade: **Média**
- CWE: **CWE-307 / CWE-400**
- Evidência: `AuthService.java:85-107` bloqueia somente pelo hash do e-mail após cinco falhas; o mesmo IP recebe dez tentativas/minuto por padrão (`application.yml:54-67`).
- Impacto: cinco tentativas contra um e-mail conhecido impedem o login legítimo por 15 minutos.
- Correção: combinar sinal por identificador e origem, atraso progressivo/desafio adaptativo, notificação e recuperação segura. Não remover proteção de força bruta.

### INFRA-08 — O CI não é ainda um gate de release completo

- Severidade: **Média**
- Evidência: `.github/workflows/ci.yml:11-30` executa apenas `verify`. `DatabaseMigrationTest.java:13` é silenciosamente ignorado sem Docker. Não há SCA, scan de segredo/imagem, build/publicação imutável, smoke test ou deploy com aprovação.
- Impacto: o build pode ficar verde sem validar PostgreSQL; vulnerabilidades e falhas de empacotamento só aparecem tarde.
- Correção: job obrigatório de integração com Docker que falha se Testcontainers não iniciar; SCA/secret scan; build e scan da imagem; staging + smoke test; promoção do mesmo digest para produção; proteção de branch.

## Checklist da skill

| Controle | Estado | Próxima ação |
|---|---|---|
| CORS sem wildcard | Pass parcial | Definir somente domínios HTTPS reais via `CORS_ALLOWED_ORIGINS`; não usar valores do `.env.example`. |
| Rate limit em todos endpoints | Fail parcial | Distribuir estado e configurar IP confiável. |
| Versionamento da API | Pass | `/api/v1`; falta política de depreciação. |
| Limite de corpo/header | Fail | Implementar no proxy e validar header Authorization na aplicação. |
| Security headers | Pass parcial | Defaults Spring; confirmar HSTS no HTTPS e política no proxy. |
| Gestão/rotação de token | Pass parcial | Secret Manager e runbook de rotação existem conceitualmente, não no deploy. |
| Erros sem stack trace | Pass | `application.yml:22-25` e handler genérico. |
| Transporte seguro | Unable to verify | TLS externo e PostgreSQL TLS não estão versionados. |
| Backup/restore | Unable to verify | Nenhum artefato operacional comprova a política. |

## Gate mínimo de produção

Antes de tráfego público, todos devem estar concluídos:

- dependências suportadas e SCA sem alta/crítica explorável;
- imagem não-root imutável e pipeline de promoção/rollback;
- Secret Manager, CORS real, TLS e proxy confiável;
- PostgreSQL privado com TLS, migration job, backup/PITR e restore testado;
- timeouts, limites, shutdown gracioso e recursos definidos;
- readiness/liveness, logs estruturados, métricas, dashboards e alertas;
- rate limit distribuído/edge e correção do lockout abusável;
- teste integrado PostgreSQL obrigatório, smoke test e teste de carga curto.
