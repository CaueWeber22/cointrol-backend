# Auditorias do Cointrol

Estes relatórios registram análises anteriores. Foram revisados documentalmente em **2026-09-27**, tomando o commit **1dce796** como referência para corrigir divergências claras. O SHA e os logs das análises originais não foram fornecidos; suas datas e resultados foram preservados como registros históricos.

| Relatório | Data declarada original | Escopo |
|---|---|---|
| [SECURITY_AUDIT.md](SECURITY_AUDIT.md) | 2026-09-06 | Superfície de ataque e controles de segurança |
| [PERFORMANCE_AUDIT.md](PERFORMANCE_AUDIT.md) | 2026-09-06 | Custos de consultas, alocações e complexidade |
| [API_INFRASTRUCTURE_AUDIT.md](API_INFRASTRUCTURE_AUDIT.md) | 2026-09-26 | API e preparação operacional |
| [RESILIENCE_AUDIT.md](RESILIENCE_AUDIT.md) | 2026-09-26 | Falhas, limites, recuperação e disponibilidade |

## Correções confirmadas no checkout

- pom.xml usa Spring Boot 4.1.1 e Java 21.
- Dockerfile possui build multi-stage e runtime não-root.
- application-prod.yml define pools, threads, timeouts Hikari e shutdown gracioso.
- A autenticação web migrou para cookies HttpOnly, CSRF sincronizado e CORS explícito; veja [o contrato frontend](../docs/FRONTEND_AUTH_COOKIES.md).
- As adendas de cada relatório distinguem essas mudanças dos achados originais. Não foi atribuída uma nova nota geral de risco.

## Validação disponível

Na implementação de autenticação desta sessão, o comando mvnw clean verify, com JDK 21, terminou com **62 testes aprovados, 0 falhas e 1 teste ignorado**. ArchUnit e os limites de cobertura passaram. DatabaseMigrationTest foi ignorado porque Docker estava indisponível.

A revisão documental não repetiu os testes, não executou SCA, benchmark, pentest, verificação de CVEs, teste de carga, restore ou validação da infraestrutura publicada. Afirmações históricas sobre suporte de versões e advisories precisam ser revalidadas nas fontes oficiais antes de orientar uma atualização.

## Publicação e manutenção

A leitura dos documentos não identificou credenciais reais, tokens de autenticação, dados pessoais de usuários ou dumps de produção. Nomes de variáveis, caminhos de código e exemplos de configuração foram mantidos; isso não substitui uma ferramenta de detecção de segredos.

Os relatórios contêm detalhes de achados e devem ser interpretados dentro do escopo histórico declarado. Ao corrigir um achado, registrar evidência, commit e validação; não confundir ausência de evidência operacional com prova de falha da infraestrutura em execução.
