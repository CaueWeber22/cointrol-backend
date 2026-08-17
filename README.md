# Cointrol

API de controle financeiro em Java 21 e Spring Boot, organizada com arquitetura hexagonal.

## Executar localmente

Pré-requisitos: Java 21 e Docker.

```powershell
docker compose up -d postgres
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

O perfil `local` usa o PostgreSQL do `compose.yml`, executa as migrations Flyway e habilita o Swagger em `http://localhost:8080/swagger-ui.html`.

## Validar

```powershell
.\mvnw.cmd verify
```

O teste PostgreSQL/Testcontainers é executado quando o Docker está disponível e é ignorado localmente quando o daemon não está ativo.

## Documentação

- [Índice da documentação](docs/README.md)
- [Implementação da estabilização](docs/IMPLEMENTACAO_ESTABILIZACAO.md)
- [Arquitetura](docs/ARQUITETURA.md)
- [Banco de dados e migrations](docs/BANCO_DE_DADOS.md)
- [Contrato da API](docs/API.md)
- [Avaliação técnica e roadmap](docs/AVALIACAO_TECNICA_E_ROADMAP.md)
