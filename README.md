# CodeTest Lab

MVP de uma plataforma para **testar código Java** em dois cenários:

1. **Modo pessoal** — cole uma classe Java + teste JUnit e execute.
2. **Professor / Aluno** — crie exercícios com testes ocultos; o aluno envia código ou projeto `.zip`; o sistema guarda o histórico.

## Stack

- Java 21
- Spring Boot 3.5.5
- Spring Web + Validation
- Spring Data JPA
- H2 (padrão) ou PostgreSQL 17
- JUnit 5 + Mockito
- Docker para sandbox de execução
- HTML/CSS/JavaScript no próprio Spring Boot

## Arquitetura resumida

```text
Browser
   |
   v
Spring Boot API
   |--- Playground
   |--- Exercises / Submissions
   |--- H2 ou PostgreSQL
   |
   v
CodeExecutor
   |
   v
Docker Sandbox
   |--- Java 21
   |--- Maven offline
   |--- JUnit 5
   `--- Mockito
```

O código enviado **não roda dentro da JVM do Spring Boot**. O backend cria um workspace temporário e chama o Docker por `ProcessBuilder`, sem shell.

## Pré-requisitos no Windows

Verifique no PowerShell:

```powershell
java -version
mvn -version
docker --version
```

Recomendado:

- Java 21
- Maven 3.9+
- Docker Desktop ativo
- IntelliJ IDEA

## 1. Criar a imagem segura de testes

Na raiz do projeto:

```powershell
docker build -t codetest-lab-runner:latest .\runner
```

A imagem baixa JUnit/Mockito durante o build. Na execução das submissões, o container roda com `--network none`.

## 2. Rodar com H2

```powershell
mvn clean test
mvn spring-boot:run
```

Acesse:

```text
http://localhost:8080
```

## 3. Rodar com PostgreSQL

Suba o banco:

```powershell
docker compose up -d postgres
```

Depois:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Credenciais padrão do Compose:

```text
Banco: codetestlab
Usuário: codetest
Senha: codetest
Porta: 5432
```

Você pode substituir com `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## Como usar o modo pessoal

A tela já abre com um exemplo de `Calculadora`.

1. Altere o código Java.
2. Altere o teste JUnit.
3. Clique em **Executar testes**.
4. Veja status, quantidade de testes, duração e saída do Maven/JUnit.

## Como usar Professor / Aluno

### Professor

Crie um exercício informando:

- título;
- descrição;
- classe esperada;
- teste JUnit oculto.

O teste oculto é persistido, mas **não é retornado pela API de consulta de exercícios**.

### Aluno — código colado

Selecione o exercício, informe nome e cole a solução Java.

### Aluno — ZIP

O arquivo deve possuir fontes em:

```text
src/main/java/
```

Exemplo:

```text
meu-projeto.zip
└── src
    └── main
        └── java
            └── Calculadora.java
```

Por segurança, o POM, scripts e testes enviados no ZIP são ignorados. O CodeTest Lab usa seu próprio POM controlado e injeta o teste oculto do professor.

## Endpoints principais

```text
GET  /api/v1/health
POST /api/v1/playground/run
POST /api/v1/exercises
GET  /api/v1/exercises
GET  /api/v1/exercises/{id}
POST /api/v1/exercises/{id}/submissions/source
POST /api/v1/exercises/{id}/submissions/zip
GET  /api/v1/exercises/{id}/submissions
```

## Controles de segurança do runner

O comando Docker é construído como lista de argumentos, sem `cmd`, `sh` ou `bash`:

```text
--network none
--read-only
--memory 384m
--cpus 1.0
--pids-limit 128
--cap-drop ALL
--security-opt no-new-privileges
--tmpfs /tmp:rw,noexec,nosuid,size=64m
```

Também existem:

- timeout de 20 segundos;
- limite de saída;
- limite de upload ZIP;
- limite de arquivos e bytes descompactados;
- proteção contra Zip Slip/path traversal;
- exclusão do workspace temporário após a execução.

> Este é um sandbox de MVP para uso local/educacional. Para disponibilizar como serviço público na internet, use isolamento adicional em infraestrutura dedicada, filas, quotas, observabilidade e políticas de segurança mais fortes.

## Testes do próprio CodeTest Lab

```powershell
mvn test
```

Há testes de:

- parser de resultados;
- construção segura do comando Docker;
- extração segura de ZIP;
- serviço de exercícios com Mockito.

## Próximos incrementos sugeridos

1. JaCoCo e dashboard de cobertura.
2. Login + papéis PROFESSOR e ALUNO.
3. Flyway para migrations PostgreSQL.
4. Fila de execução com Kafka.
5. IA para explicar falhas e sugerir estudos.
6. Ranking/turma e exportação de resultados.
7. Runners Python e JavaScript.

## Estrutura

```text
codetest-lab/
├── runner/                     # imagem Docker que executa JUnit
├── src/main/java/              # backend Spring Boot
├── src/main/resources/static/  # interface web
├── src/test/java/              # JUnit + Mockito
├── tools/                      # smoke test sem dependências externas
├── docs/superpowers/           # design e plano do MVP
├── docker-compose.yml          # PostgreSQL opcional
└── pom.xml
```
