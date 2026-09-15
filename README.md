# CodeTest Lab

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-Testing-78A641)
![Docker](https://img.shields.io/badge/Docker-Sandbox-2496ED?logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?logo=apachemaven&logoColor=white)

> Plataforma educacional para execução segura e automatizada de testes em código Java, com foco em qualidade de software, ensino e práticas de backend.

## 🎯 Sobre o projeto

O **CodeTest Lab** é uma aplicação desenvolvida em Java 21 e Spring Boot para executar testes automatizados em código Java dentro de um ambiente Docker isolado.

O projeto nasceu com dois objetivos principais:

- oferecer um **modo pessoal**, onde o desenvolvedor pode colar código Java e executar testes JUnit;
- oferecer um **modo Professor / Aluno**, onde exercícios podem ser criados com testes ocultos e os alunos enviam suas soluções por código ou arquivo `.zip`.

O código submetido não é executado diretamente na JVM do backend. O sistema cria um workspace temporário e delega a execução para um container Docker controlado.

## ✨ Principais funcionalidades

- execução de código Java com **JUnit 5**;
- suporte a **Mockito** nos testes;
- execução isolada em **Docker Sandbox**;
- modo pessoal para testes rápidos;
- criação de exercícios para professores;
- testes ocultos para avaliação automática;
- submissão de soluções por código-fonte;
- submissão de projetos `.zip`;
- histórico de submissões;
- API REST com Spring Boot;
- persistência com H2 ou PostgreSQL;
- proteção contra Zip Slip / path traversal;
- timeout, limite de saída, CPU, memória e processos no runner;
- processamento seguro via `ProcessBuilder`, sem execução por shell.

## 🧠 Skills demonstradas neste projeto

Este projeto demonstra, na prática, conhecimentos relevantes para desenvolvimento Java Backend e qualidade de software:

### Backend

- Java 21
- Spring Boot 3.5.5
- Spring Web
- APIs REST
- Bean Validation
- Spring Data JPA
- Hibernate
- Maven
- arquitetura em camadas
- tratamento e validação de entrada

### Bancos de dados

- PostgreSQL 17
- H2
- persistência com JPA
- modelagem de entidades
- repositories

### Testes e qualidade

- JUnit 5
- Mockito
- testes unitários
- assertions
- mocks
- TDD
- testes automatizados
- análise de falhas de execução
- Maven Surefire

### DevOps e segurança

- Docker
- Docker Compose
- sandbox para execução de código
- limitação de CPU e memória
- isolamento de rede
- filesystem read-only
- `no-new-privileges`
- `cap-drop ALL`
- proteção contra Zip Slip
- execução de processos com `ProcessBuilder`

### Desenvolvimento e engenharia

- Git
- GitHub
- debugging
- refatoração incremental
- tratamento de erros
- processamento de arquivos ZIP
- organização de projeto Maven
- documentação técnica

## 🏗 Arquitetura

```text
Browser
   |
   v
Spring Boot API
   |
   |--- Playground
   |--- Exercises
   |--- Submissions
   |--- H2 / PostgreSQL
   |
   v
CodeExecutor
   |
   v
Docker Sandbox
   |
   |--- Java 21
   |--- Maven Offline
   |--- JUnit 5
   `--- Mockito
```

## 🔄 Fluxo de execução

```text
Usuário envia código
        |
        v
Spring Boot recebe a requisição
        |
        v
Workspace temporário é criado
        |
        v
Código + teste são preparados
        |
        v
Docker Sandbox executa Maven/JUnit
        |
        v
Resultado é processado
        |
        v
API devolve status, métricas e log
```

## 🧪 Cenários já validados

### ✅ Teste aprovado

Exemplo:

```java
assertEquals(15, calculadora.somar(10, 5));
```

Resultado esperado:

```text
PASSED
Testes: 1
Passaram: 1
Falharam: 0
```

### ❌ Teste com falha de assertion

Exemplo propositalmente incorreto:

```java
assertEquals(14, calculadora.somar(10, 5));
```

Resultado identificado pelo sistema:

```text
FAILED
expected: <14> but was: <15>
```

Isso confirma que o CodeTest Lab diferencia corretamente um código que compila, mas não atende ao comportamento esperado pelo teste.

### ⚠ Próximo cenário de validação

- erro de compilação Java;
- identificação amigável de `COMPILE ERROR`;
- apresentação estruturada da linha e causa do erro.

## 🔐 Segurança do runner

O container de execução utiliza controles como:

```text
--network none
--read-only
--memory 384m
--cpus 1.0
--pids-limit 128
--cap-drop ALL
--security-opt no-new-privileges
```

Também são aplicados:

- timeout de execução;
- limite de saída;
- limite de upload ZIP;
- limite de arquivos e bytes extraídos;
- proteção contra path traversal;
- exclusão do workspace temporário ao final da execução.

> O sandbox atual foi projetado para uso local e educacional. Para exposição pública na internet, o projeto deverá evoluir com filas, quotas, isolamento adicional, observabilidade e políticas de segurança mais restritivas.

## 🧰 Stack

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 3.5.5 | Backend e API REST |
| Spring Web | Endpoints HTTP |
| Spring Data JPA | Persistência |
| Hibernate | ORM |
| JUnit 5 | Testes automatizados |
| Mockito | Mocks e isolamento de dependências |
| PostgreSQL 17 | Banco relacional |
| H2 | Banco em memória para desenvolvimento |
| Docker | Sandbox de execução |
| Docker Compose | Infraestrutura local |
| Maven | Build e dependências |
| HTML / CSS / JavaScript | Interface web |

## 🚀 Como executar

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker Desktop
- IntelliJ IDEA ou outra IDE Java

### Windows / PowerShell

Na raiz do projeto:

```powershell
.\start.ps1
```

O script:

1. verifica o Docker;
2. constrói ou atualiza a imagem do runner;
3. executa os testes do projeto;
4. inicia a aplicação Spring Boot.

Depois acesse:

```text
http://localhost:8080
```

## 🗄 PostgreSQL

Para usar PostgreSQL:

```powershell
docker compose up -d postgres
```

Depois:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Configuração padrão:

```text
Database: codetestlab
User: codetest
Password: codetest
Port: 5432
```

Também é possível utilizar:

```text
DB_URL
DB_USER
DB_PASSWORD
```

## 📡 Endpoints principais

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

## 👨‍🏫 Modo Professor / Aluno

### Professor

O professor pode criar um exercício definindo:

- título;
- descrição;
- classe esperada;
- teste JUnit oculto.

O teste oculto é persistido, mas não é retornado pela API pública de consulta dos exercícios.

### Aluno — código-fonte

O aluno seleciona o exercício, informa seu nome e envia a implementação Java.

### Aluno — projeto ZIP

Estrutura esperada:

```text
meu-projeto.zip
└── src
    └── main
        └── java
            └── Calculadora.java
```

O CodeTest Lab ignora POMs, scripts e testes enviados pelo aluno e utiliza sua própria configuração controlada.

## 📁 Estrutura do projeto

```text
codetest-lab/
├── runner/                     # imagem Docker do executor
├── src/main/java/              # backend Spring Boot
├── src/main/resources/static/  # interface web
├── src/test/java/              # testes JUnit + Mockito
├── tools/                      # smoke tests auxiliares
├── docs/                       # documentação de arquitetura e evolução
├── docker-compose.yml          # PostgreSQL local
├── pom.xml                     # dependências Maven
├── start.ps1                   # inicialização no Windows
└── README.md
```

## 🧪 Testes do próprio projeto

Execute:

```powershell
mvn test
```

O projeto possui testes para componentes como:

- parser de resultados;
- construção segura do comando Docker;
- leitura de relatórios Surefire;
- extração segura de arquivos ZIP;
- regras do serviço de exercícios com Mockito.

## 🛣 Roadmap

- [x] MVP Spring Boot
- [x] execução segura em Docker
- [x] JUnit 5
- [x] Mockito
- [x] envio de código-fonte
- [x] envio por ZIP
- [x] modo Professor / Aluno
- [x] detecção de assertion failure
- [ ] detecção estruturada de erro de compilação
- [ ] JaCoCo e dashboard de cobertura
- [ ] autenticação e autorização PROFESSOR / ALUNO
- [ ] PostgreSQL com Flyway
- [ ] dashboard visual de execuções
- [ ] fila de execução com Kafka
- [ ] observabilidade com métricas e logs
- [ ] IA para explicar falhas de testes
- [ ] ranking e turmas
- [ ] runners para Python e JavaScript

## 🎓 Objetivo educacional

Além de ser um projeto de portfólio Java Backend, o CodeTest Lab foi pensado como ferramenta de apoio ao ensino de programação e testes de software.

A plataforma permite demonstrar na prática conceitos como:

- testes unitários;
- Arrange, Act, Assert;
- assertions;
- mocks;
- testes positivos e negativos;
- análise de stack trace;
- boas práticas de desenvolvimento;
- execução isolada e segura de código.

## 👨‍💻 Autor

**Jucelio Farias Coelho**

GitHub: [@juceliocoelho2022](https://github.com/juceliocoelho2022)

---

Se este projeto for útil para seus estudos ou para ensino de programação, acompanhe a evolução pelo repositório e pelas próximas releases.
