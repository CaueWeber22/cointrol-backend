# Contexto para agentes: Cointrol Backend

Este repositório é o backend do Cointrol: uma API de controle financeiro pessoal em Java 21 e Spring Boot. O sistema permite que usuários autenticados gerenciem contas, categorias, receitas, despesas, transferências, saldos e resumos financeiros.

## Como trabalhar neste repositório

- Preserve a arquitetura hexagonal existente.
- Mantenha regras de negócio em `src/main/java/com/fcproject/application/core`.
- Mantenha contratos de entrada e saída em `src/main/java/com/fcproject/application/ports`.
- Mantenha HTTP, controllers e DTOs em `src/main/java/com/fcproject/adapters/inbound`.
- Mantenha JPA, repositories, entities, mappers e integrações externas em `src/main/java/com/fcproject/adapters/outbound`.
- Mantenha configuração Spring, segurança, filtros, OpenAPI e tratamento global de erros em `src/main/java/com/fcproject/infrastructure`.
- Não faça o núcleo da aplicação depender de Spring MVC, JPA, Jackson, controllers, DTOs HTTP ou adapters.
- Não coloque segredos, tokens, senhas ou valores de ambiente em arquivos versionados.

## Documentos de contexto

Leia os documentos certos antes de alterar o comportamento:

- `docs/BUSINESS_RULES.md`: regras de negócio de autenticação, usuários, contas, categorias, lançamentos, transferências, saldos e resumos.
- `docs/ARCHITECTURE.md`: mapa operacional da arquitetura do backend para agentes.
- `docs/DESIGN.md`: decisões de design de API, domínio, segurança e persistência.
- `docs/API.md`: contrato detalhado dos endpoints, payloads, respostas e erros.
- `docs/FRONTEND_AUTH_COOKIES.md`: cookies HttpOnly, CSRF, CORS e contrato de autenticação web.
- `docs/ARQUITETURA.md`: documentação técnica mais detalhada da arquitetura hexagonal.
- `docs/BANCO_DE_DADOS.md`: schemas PostgreSQL, migrations Flyway, integridade financeira e política de evolução.
- `docs/SEGURANCA.md`: autenticação, refresh tokens, rate limit, bloqueio de login, auditoria e rotação de JWT.

Quando um documento existente for mais específico que este arquivo, siga o documento específico.

## Comandos úteis

Subir o PostgreSQL local:

```powershell
docker compose up -d postgres
```

Rodar a API localmente:

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

Validar o projeto:

```powershell
.\mvnw.cmd verify
```

Rodar uma classe de teste específica:

```powershell
.\mvnw.cmd -Dtest=ClassNameTest test
```

## Regras para mudanças

- Antes de mudar regra financeira, leia `docs/BUSINESS_RULES.md`.
- Antes de mudar endpoints, DTOs, códigos de erro ou semântica HTTP, leia `docs/API.md`.
- Antes de mudar banco, entities ou repositories, leia `docs/BANCO_DE_DADOS.md` e crie a próxima migration Flyway.
- Antes de mudar autenticação, autorização, JWT, refresh token, rate limit, auditoria ou headers de segurança, leia `docs/SEGURANCA.md`.
- Para novas features, siga a trilha: controller/DTO -> inbound port -> use case -> outbound port quando necessário -> adapter/entity/repository -> migration -> testes.
- Não edite migrations já aplicadas em ambientes compartilhados. Crie sempre a próxima versão `Vn__descricao.sql`.
- Preserve o escopo por usuário em todos os recursos financeiros.
- Preserve idempotência em criação de lançamentos e transferências.
- Preserve atomicidade em transferências e saldo inicial de conta.

## Testes esperados

- Controller tests para contrato HTTP, status codes e serialização.
- Use case tests para regras de negócio.
- Adapter tests para persistência, segurança e integrações.
- Migration/Testcontainers tests para evolução de banco.
- ArchUnit tests para proteger os limites da arquitetura hexagonal.

O teste PostgreSQL com Testcontainers pode exigir Docker. Se Docker não estiver disponível localmente, informe isso em vez de mascarar falhas.
