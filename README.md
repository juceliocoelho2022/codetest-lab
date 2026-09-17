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

- oferecer um **modo pessoal**, onde o desenvolvedor pode trabalhar com até 10 arquivos Java em abas e executar testes com JUnit, Mockito e JaCoCo;
- oferecer um **modo Professor / Aluno**, onde exercícios podem ser criados com testes ocultos e os alunos enviam suas soluções por código ou arquivo `.zip`.

O código submetido não é executado diretamente na JVM do backend. O sistema cria um workspace temporário e delega a execução para um container Docker controlado.

## ✨ Principais funcionalidades

- execução de código Java com **JUnit 5**;
- suporte a **Mockito** nos testes;
- cobertura com **JaCoCo** para linhas, métodos, branches e classes;
- execução isolada em **Docker Sandbox**;
- modo pessoal com **até 10 arquivos Java em abas**;
- limite agregado de 100.000 caracteres no workspace pessoal;
- navegação de erros de compilação para o arquivo e linha correspondentes;
- diagnóstico estruturado de `PASSED`, `FAILED`, `COMPILE_ERROR`, `TIMEOUT` e `INFRASTRUCTURE_ERROR`;
- Docker Health com estados `UP`, `DEGRADED` e `DOWN`;
- Execution Guard que bloqueia execuções quando Docker/runner não estão prontos;
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
- JaCoCo
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
- health check do ambiente de execução

### Desenvolvimento e engenharia

- Git
- GitHub
- debugging
- refatoração incremental
- tratamento de erros
- processamento de arquivos ZIP
- workspace Java multi-file
- organização de projeto Maven
- documentação técnica

## 🏗 Arquitetura

```text
Browser
   |
   |--- sourceFiles[]
   |--- testCode
   |--- executionProfile
   v
Spring Boot API
   |
   |--- Playground
   |--- Exercises
   |--- Submissions
   |--- Docker Health
   |--- H2 / PostgreSQL
   |
   v
CodeExecutor
   |
   v
Workspace Maven temporário
   |
   v
Docker Sandbox
   |
   |--- Java 21
   |--- Maven Offline
   |--- JUnit 5
   |--- Mockito
   `--- JaCoCo
```

## 🔄 Fluxo de execução

```text
Usuário organiza um ou mais arquivos Java
        |
        v
Browser envia sourceFiles[] + teste + perfil
        |
        v
Spring Boot valida a requisição
        |
        v
Workspace temporário é criado
        |
        v
Arquivos Java + teste são materializados
        |
        v
Docker Sandbox executa Maven/JUnit
        |
        v
Resultado e cobertura são processados
        |
        v
API devolve status, métricas, cobertura e log
```

## 🧪 Cenários já validados

### ✅ Teste aprovado

Exemplo:

```java
assertEquals(15, calculadora.somar(10, 5));
```

Resultado:

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

### ⚠ Erro de compilação estruturado

O sistema identifica arquivo, linha, coluna e mensagem do compilador e oferece navegação para a linha correspondente no editor.

Exemplo de status:

```text
COMPILE_ERROR
Arquivo: PedidoService.java
Linha: 3
Coluna: 19
Problema: cannot find symbol
```

### ✅ Multi-file + Mockito

Cenário validado com:

```text
PedidoService.java
EstoqueRepository.java
PedidoServiceTest.java
```

usando `JUnit 5 + Mockito` dentro do runner Docker, com resultado:

```text
PASSED
1 executado
1 aprovado
0 falhas
```

### ✅ JaCoCo

O modo pessoal também foi validado com cobertura JaCoCo exibindo percentuais de linhas, métodos, branches e classes.

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
| JaCoCo | Cobertura de código |
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
GET  /api/v1/health/runner
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
node tools/ui-smoke.mjs
node tools/cache-busting-smoke.mjs
```

O projeto possui testes para componentes como:

- parser de resultados;
- construção segura do comando Docker;
- Docker Health;
- perfis de execução;
- leitura de relatórios Surefire e JaCoCo;
- extração segura de arquivos ZIP;
- normalização e validação de múltiplos arquivos Java;
- criação segura do workspace Maven;
- contrato multi-file do playground;
- smoke tests da interface e cache-busting;
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
- [x] detecção estruturada de erro de compilação
- [x] JaCoCo e dashboard de cobertura
- [x] Docker Health + Execution Guard
- [x] playground Java multi-file em abas
- [ ] múltiplos arquivos de teste
- [ ] packages Java e árvore de projeto
- [ ] autenticação e autorização PROFESSOR / ALUNO
- [ ] PostgreSQL com Flyway
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
- cobertura de código;
- organização de múltiplas classes Java;
- boas práticas de desenvolvimento;
- execução isolada e segura de código.

## 👨‍💻 Autor

**Jucelio Farias Coelho**

GitHub: [@juceliocoelho2022](https://github.com/juceliocoelho2022)

---

Se este projeto for útil para seus estudos ou para ensino de programação, acompanhe a evolução pelo repositório e pelas próximas releases.
