# Auditoria inicial de segurança

> **Relatório histórico.** Revisão documental em 2026-09-27, confrontada com o commit 1dce796. A data abaixo é a registrada no documento original; o SHA auditado originalmente não foi informado. Notas de risco, números de linha e resultados antigos não devem ser tratados como validação atual de produção.

## Atualização de contexto — 2026-09-27

- SEC-01: a evidência de Spring Boot 3.4.2 foi superada; pom.xml agora usa 4.1.1. Isso não equivale a uma varredura SCA nem a comprovação de ausência de vulnerabilidades.
- A autenticação web agora usa cookies HttpOnly e CSRF obrigatório, com sessão exclusiva para CSRF; o filtro JWT executa antes de CsrfFilter. O checklist de CSRF abaixo descreve somente o modelo Bearer anterior.
- SEC-05: refresh/logout deixaram de exigir JSON e RefreshTokenRequest foi removido. Login/cadastro ainda recebem JSON; limites efetivos de proxy não foram validados nesta revisão.
- A reprodução de falhas de login agora exige obter um CSRF válido e preservar a sessão. A proteção de login e o rate limiter continuam exigindo avaliação dos riscos descritos.
- O perfil prod limita o mapa do rate limiter a 10.000 chaves por padrão; o padrão geral continua 100.000.

Consulte [o índice de auditorias](README.md) para escopo, limitações e validação recente.

## Relatório original

Data: 2026-09-06
Escopo: backend completo (`src/main`, migrations, configuração e testes)
Método: revisão estática orientada a superfície de ataque e execução de `mvnw verify`.

## Resumo executivo

Risco geral: **6,0/10 (moderado)**.

Não foi encontrada evidência de SQL injection, exposição direta de segredo, IDOR ou quebra de validação de JWT. O escopo por usuário está aplicado nas consultas financeiras e também é reforçado por chaves estrangeiras compostas. Os riscos mais relevantes são de disponibilidade: bloqueio deliberado de contas, exaustão do rate limiter e endpoints analíticos sem limite de período/custo. A plataforma Spring Boot usada também está fora do suporte open source.

Prioridade recomendada:

1. Atualizar Spring Boot 3.4.2 para uma linha suportada e executar uma varredura automatizada de dependências no CI.
2. Redesenhar o bloqueio de login para não permitir negação de serviço contra um e-mail conhecido.
3. Substituir o rate limiter local por gateway/Redis ou cache com expiração e política de capacidade segura.
4. Limitar o intervalo dos resumos e agregar saldos/resumos no PostgreSQL.
5. Aplicar limite de corpo HTTP no proxy/gateway e, por defesa em profundidade, na aplicação.

## Superfície de ataque observada

- Entrada: `Startup` inicia Spring MVC; controllers expõem `/api/v1/auth`, `/api/v1/users`, `/api/v1/accounts`, `/api/v1/categories`, `/api/v1/transactions`, `/api/v1/transfers` e `/api/v1/summary`.
- Rotas públicas: login, refresh, logout, cadastro, health/info e, quando habilitada, documentação OpenAPI (`SecurityConfig.java:59-64`).
- Cadeia relevante: `RateLimitFilter` antes do contexto de segurança; `JwtAuthenticationFilter` antes do filtro de usuário/senha (`SecurityConfig.java:66-68`).
- Banco: PostgreSQL via JPA/JdbcTemplate. As consultas JDBC encontradas são parametrizadas.
- Integrações externas: nenhuma encontrada.
- Upload de arquivos: nenhum encontrado.
- Sessão: stateless; access token JWT HMAC-SHA-256; refresh token opaco persistido por SHA-256.

## Achados

### SEC-01 — Versão base do Spring Boot fora de suporte

- Severidade: **Alta**
- CWE: **CWE-1104 — Use of Unmaintained Third Party Components**
- Evidência: `pom.xml:5-9` fixa `spring-boot-starter-parent` em `3.4.2`.
- Por que importa: a última versão open source da linha 3.4 foi 3.4.13, e a própria equipe Spring recomenda migrar para 3.5.x ou 4.0.x. Permanecer em 3.4.2 impede receber correções acumuladas e deixa o BOM muito atrás dos patches de framework, servidor, serialização e persistência.
- Exploração: depende de quais vulnerabilidades das dependências transitivas são alcançáveis pela configuração. A revisão não atribui automaticamente CVE-2026-22731 ao projeto: o advisory exige um health group com caminho adicional, configuração que não foi encontrada aqui.
- Correção: migrar primeiro para a última versão de patch compatível disponível no canal usado pela equipe e, preferencialmente, para uma linha com suporte open source. Em seguida executar testes de contrato, integração e migration.
- Defesa em profundidade: adicionar Dependabot/Renovate e SCA no CI com falha para vulnerabilidades altas/críticas.
- Referências: [fim do suporte open source do Spring Boot 3.4.x](https://spring.io/blog/2025/12/18/spring-boot-3-4-13-available-now/), [advisory oficial CVE-2026-22731 e versões corrigidas](https://spring.io/security/cve-2026-22731/).

### SEC-02 — Bloqueio de login permite negar acesso a uma conta conhecida

- Severidade: **Média**
- CWE: **CWE-307 — Improper Restriction of Excessive Authentication Attempts** / **CWE-400 — Uncontrolled Resource Consumption**
- Evidência: `AuthService.java:85-107` consulta e incrementa o estado somente pelo hash do e-mail; após o limite, o identificador é bloqueado. Os padrões são 5 falhas e 15 minutos (`application.yml:54-58`). O rate limit de login permite 10 tentativas por minuto/IP (`application.yml:65-67`), portanto um único IP alcança o bloqueio da vítima.
- Por que importa: sabendo o e-mail de um usuário, um atacante pode provocar cinco falhas e impedir o login legítimo por 15 minutos. A ação pode ser repetida após o desbloqueio e distribuída por IPs.
- Reprodução segura: em ambiente local, enviar cinco `POST /api/v1/auth/login` com um e-mail existente e senha incorreta; a tentativa seguinte do titular recebe `429 LOGIN_TEMPORARILY_BLOCKED`.
- Correção: contabilizar risco por combinação de identificador, IP/dispositivo e histórico; preferir atraso progressivo, desafio adaptativo e notificação ao usuário. Se o bloqueio rígido for mantido, o limite por IP precisa ficar abaixo da quantidade necessária para bloquear múltiplas contas e deve existir recuperação segura.
- Defesa em profundidade: alertar sobre campanhas que atingem muitos identificadores a partir de uma origem ou o mesmo identificador a partir de muitas origens.

### SEC-03 — Capacidade do rate limiter pode causar negação global e o limite é contornável entre réplicas

- Severidade: **Média**
- CWE: **CWE-400 — Uncontrolled Resource Consumption**
- Evidência: `FixedWindowRateLimiter.java:30-35` nega toda chave nova quando `windows.size()` atinge `maximumTrackedKeys`; o padrão é 100.000 (`application.yml:62-64`). A limpeza percorre o mapa somente a cada 512 aquisições (`FixedWindowRateLimiter.java:55-60`). O estado é local à JVM (`FixedWindowRateLimiter.java:12-14`).
- Por que importa: muitas chaves de origem distintas podem preencher o mapa e negar clientes ainda não vistos até haver expiração/limpeza. Em múltiplas réplicas, o atacante também pode multiplicar o limite alternando instâncias.
- Reprodução segura: teste unitário com capacidade mínima de 100; adquirir uma janela longa para 100 chaves distintas e verificar que a 101ª é negada mesmo sem exceder sua própria cota.
- Correção: usar rate limiting no gateway ou Redis com operação atômica e TTL. Se o cache local continuar, adotar expiração automática e uma política de admissão/evicção que não transforme falta de capacidade em bloqueio global silencioso.
- Defesa em profundidade: monitorar cardinalidade, evicções e rejeições por causa; documentar corretamente a resolução do IP atrás de proxies confiáveis.

### SEC-04 — Resumos sem limite de intervalo permitem exaustão autenticada de CPU/memória

- Severidade: **Média**
- CWE: **CWE-400 — Uncontrolled Resource Consumption**
- Evidência: `SummaryController.java:40-67` aceita qualquer par `from`/`to`; `FinanceSummaryUtil.java:22-33` valida apenas ordem e carrega todas as contas, categorias e todos os lançamentos confirmados do intervalo. `FinancePersistenceAdapter.java:149-151` materializa o resultado inteiro, e os três casos de uso agregam na JVM.
- Por que importa: um usuário com histórico grande pode solicitar intervalos arbitrariamente amplos; uma única requisição pode gerar alto tráfego do banco, muitas alocações e pausas de GC, afetando outros usuários.
- Reprodução segura: criar grande volume de lançamentos para um usuário de teste e comparar heap/latência de `/api/v1/summary?from=1900-01-01&to=9999-12-31` com um intervalo mensal.
- Correção: impor janela máxima de produto e executar `SUM ... GROUP BY` no PostgreSQL, retornando apenas os agregados necessários.
- Defesa em profundidade: timeout de consulta, orçamento de resposta e métricas de linhas lidas por endpoint.

### SEC-05 — Não há limite verificável para corpos JSON ou header Authorization

- Severidade: **Baixa** (Média se não houver limite no proxy)
- CWE: **CWE-400 — Uncontrolled Resource Consumption**
- Evidência: `application.yml:22-35` não configura limites de requisição; `AuthController.java:25-50` expõe três corpos JSON públicos. Os DTOs limitam campos após a desserialização (`LoginCredentialsRequest.java:7-10`, `RefreshTokenRequest.java:6-8`), mas `JwtAuthenticationFilter.java:46-55` não limita o tamanho do header antes de decodificar.
- Por que importa: Bean Validation ocorre depois de o corpo ter sido recebido/desserializado. Sem um proxy com limite, payloads ou tokens anormalmente grandes consomem memória e CPU antes da rejeição.
- Exploração: enviar corpo JSON ou bearer token com vários megabytes. O impacto exato depende dos limites do balanceador/proxy, que não estão no repositório.
- Correção: impor limite de request/header no gateway/ingress e rejeitar `Authorization` acima de um tamanho pequeno antes de `JWT.decode`. Para corpo JSON, usar filtro/container/gateway compatível com o ambiente de implantação.
- Defesa em profundidade: timeout de leitura, limite de conexões e métricas de status 413/431.

## Controles positivos verificados

- JWT valida assinatura, emissor, audiência, expiração e `kid` (`JwtTokenAdapter.java:74-84`, `128-132`).
- Refresh tokens têm 512 bits aleatórios, são armazenados por hash e rotacionados sob lock pessimista (`AuthService.java:128-151`; `RefreshTokenAdapters.java:43-56`).
- Consultas financeiras incluem `userId`, e FKs compostas impedem relações entre proprietários (`V7__create_financial_entries.sql:17-22`).
- CORS usa origens explícitas e não aceita curingas por padrão (`SecurityConfig.java:97-112`).
- Erros inesperados não retornam stack trace nem detalhe interno (`application.yml:22-25`; `GlobalHandler.java:106-114`).
- Senhas usam BCrypt com custo 12 (`SecurityConfig.java:85-88`).
- Não foram encontrados uploads, desserialização Java nativa, execução de processos ou SQL concatenado.

## Checklist

| Controle | Resultado | Observação |
|---|---|---|
| Autenticação e validação JWT | Pass | Assinatura, issuer, audience, expiração e estado do usuário são verificados. |
| Autorização/escopo por usuário | Pass | Consultas e FKs financeiras preservam ownership. |
| Refresh token | Pass parcial | Rotação e hash corretos; falta limpeza de tokens antigos, tratada no relatório de performance. |
| Proteção de login | Fail parcial | Evita força bruta, mas permite lockout abusivo. |
| Rate limiting | Fail parcial | Capacidade fail-closed e estado por instância. |
| Validação de entrada | Pass parcial | DTOs limitam campos; não há limite de custo/intervalo ou de corpo verificável. |
| SQL injection | Pass | JdbcTemplate parametrizado e JPQL com parâmetros. |
| Segredos versionados | Pass | Apenas defaults locais/placeholder foram encontrados. |
| CORS | Pass | Lista explícita; configuração de produção depende do ambiente. |
| CSRF | Pass para o modelo atual | API stateless com bearer token; reavaliar se tokens forem movidos para cookies. |
| Security headers | Pass parcial | Defaults do Spring Security; CSP e política do proxy não são verificáveis. |
| Upload de arquivos | N/A | Nenhum fluxo encontrado. |
| TLS/proxy confiável | Unable to verify | Infraestrutura de produção não está no repositório. |
| Dependências vulneráveis | Fail parcial | BOM antigo/EOL; SCA completo não foi executado. |
| Logs/auditoria sem credenciais | Pass | Não foi encontrado log de senha ou token bruto. |

## Validação

`mvnw verify`: **55 testes passaram, 0 falharam, 1 foi ignorado**. O teste `DatabaseMigrationTest` foi ignorado porque não havia ambiente Docker disponível; por isso, a aplicação real das migrations em PostgreSQL não foi revalidada nesta auditoria.
