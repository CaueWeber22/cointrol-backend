# Cointrol

API de controle financeiro em Java 21 e Spring Boot, organizada com arquitetura hexagonal.

## Features atuais

- cadastro, login JWT, refresh, logout e perfil;
- contas financeiras com saldo inicial por lançamento;
- categorias de receita e despesa;
- receitas, despesas, extrato paginado e cancelamento;
- saldos confirmado, pendente e projetado;
- transferências atômicas e idempotentes;
- resumos por moeda, categoria e mês.

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
- [Implementação do MVP financeiro](docs/IMPLEMENTACAO_MVP_FINANCEIRO.md)
- [Guia de features financeiras](docs/GUIA_IMPLEMENTACAO_FEATURES_FINANCEIRAS.md)
- [Arquitetura](docs/ARQUITETURA.md)
- [Banco de dados e migrations](docs/BANCO_DE_DADOS.md)
- [Contrato da API](docs/API.md)
- [Avaliação técnica e roadmap](docs/AVALIACAO_TECNICA_E_ROADMAP.md)
