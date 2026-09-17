# CodeTest Lab

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-5.15.2-78A641)
![JaCoCo](https://img.shields.io/badge/JaCoCo-0.8.12-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Sandbox-2496ED?logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?logo=apachemaven&logoColor=white)
![Version](https://img.shields.io/badge/MVP-0.4.0-blue)

> Plataforma educacional para execução segura e automatizada de testes em código Java, com foco em Java Backend, qualidade de software, ensino e sandbox de execução.

## 🚀 Versão atual — MVP 0.4.0

A versão **0.4.0** introduz o **Multi-File Playground** no modo pessoal. Agora é possível trabalhar com várias classes Java no mesmo exercício, executar testes JUnit 5 com Mockito e visualizar cobertura JaCoCo, mantendo a execução isolada em Docker.

Principais capacidades da versão atual:

- até **10 arquivos Java** no workspace pessoal;
- limite agregado de **100.000 caracteres** de código-fonte;
- abas para criar, alternar, renomear e excluir arquivos;
- classe principal validada contra o arquivo correspondente;
- perfis de execução com JUnit 5, Mockito e JaCoCo;
- diagnóstico estruturado de falhas de compilação;
- navegação para arquivo e linha do erro;
- Docker Health e Execution Guard;
- cache-busting dos assets da interface (`?v=0.4.0`).

## 🎯 Objetivo do projeto

O **CodeTest Lab** foi criado para demonstrar, em um único projeto, práticas de backend Java, testes automatizados, segurança de execução e apoio ao ensino de programação.

A aplicação possui dois fluxos principais:

### Modo pessoal

O desenvolvedor organiza seus arquivos Java em abas, escolhe um perfil de testes, escreve um teste JUnit e executa tudo dentro do sandbox Docker.

### Modo Professor / Aluno

O professor cria exercícios com testes ocultos. O aluno envia uma solução por código-fonte ou por arquivo `.zip`, e a plataforma executa a avaliação automaticamente sem expor o teste oculto.

## ✨ Funcionalidades

- Java 21 com Spring Boot 3.5.5;
- API REST para playground, exercícios, submissões e health check;
- JUnit 5 para testes automatizados;
- Mockito para mocks, stubs e verificações;
- JaCoCo para cobertura de linhas, métodos, branches e classes;
- workspace pessoal multi-file;
- suporte temporário ao payload legado `sourceCode` durante a transição para `sourceFiles[]`;
- diagnóstico de `PASSED`, `FAILED`, `COMPILE_ERROR`, `TIMEOUT` e `INFRASTRUCTURE_ERROR`;
- navegação de erro de compilação por arquivo/linha;
- histórico de submissões;
- H2 por padrão e PostgreSQL 17 opcional;
- Docker Health com estados `UP`, `DEGRADED` e `DOWN`;
- Execution Guard para bloquear execuções quando Docker/runner não estão disponíveis;
- proteção contra Zip Slip e path traversal;
- limites de CPU, memória, processos, timeout e tamanho de saída;
- execução com `ProcessBuilder`, sem shell intermediário;
- Maven offline dentro do runner controlado.

## 🧪 Perfis de execução

| Perfil | JUnit 5 | Mockito | JaCoCo |
|---|:---:|:---:|:---:|
| `JUNIT5` | ✅ | — | — |
| `JUNIT5_MOCKITO` | ✅ | ✅ | — |
| `JUNIT5_JACOCO` | ✅ | — | ✅ |
| `JUNIT5_MOCKITO_JACOCO` | ✅ | ✅ | ✅ |

O perfil padrão mantém compatibilidade com **JUnit 5 + Mockito**. Perfis com JaCoCo recebem timeout mínimo maior para acomodar a geração do relatório de cobertura.

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
   |--- PlaygroundController
   |--- Exercises / Submissions
   |--- Runner Health
   |--- H2 / PostgreSQL
   v
ExecutionRequest
   |
   v
WorkspaceFactory
   |
   |--- src/main/java/*.java
   |--- src/test/java/*Test.java
   |--- pom.xml controlado
   v
CodeExecutor
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
Spring Boot valida nomes, limites e classe principal
        |
        v
Workspace Maven temporário é criado
        |
        v
Arquivos Java + teste são materializados
        |
        v
Docker executa Maven/JUnit em sandbox
        |
        v
Surefire/JaCoCo geram os resultados
        |
        v
Backend normaliza status, diagnóstico e cobertura
        |
        v
Interface apresenta resultado amigável
```

## 🧩 Exemplo multi-file validado

### `PedidoService.java`

```java
public class PedidoService {

    private final EstoqueRepository repository;

    public PedidoService(EstoqueRepository repository) {
        this.repository = repository;
    }

    public boolean realizar(String produto, int quantidade) {
        if (!repository.temEstoque(produto, quantidade)) {
            return false;
        }

        repository.retirarEstoque(produto, quantidade);
        return true;
    }
}
```

### `EstoqueRepository.java`

```java
public interface EstoqueRepository {

    boolean temEstoque(String produto, int quantidade);

    void retirarEstoque(String produto, int quantidade);
}
```

### `PedidoServiceTest`

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    @Test
    void deveRetirarEstoqueQuandoDisponivel() {
        EstoqueRepository repository = mock(EstoqueRepository.class);

        when(repository.temEstoque("Notebook", 1))
                .thenReturn(true);

        PedidoService service = new PedidoService(repository);

        assertTrue(service.realizar("Notebook", 1));

        verify(repository).retirarEstoque("Notebook", 1);
    }
}
```

Resultado validado no runner Docker:

```text
PASSED
1 executado
1 aprovado
0 falhas
```

## 📊 Cobertura JaCoCo

Nos perfis com JaCoCo, o CodeTest Lab lê o relatório XML gerado em:

```text
target/site/jacoco/jacoco.xml
```

A interface apresenta, quando disponíveis:

- cobertura de linhas;
- cobertura de métodos;
- cobertura de branches;
- cobertura de classes.

## 🧭 Diagnóstico de compilação

Quando o compilador retorna um erro, a aplicação estrutura o diagnóstico em vez de exibir apenas o log bruto.

Exemplo:

```text
COMPILE_ERROR
Arquivo: PedidoService.java
Linha: 3
Coluna: 19
Problema: cannot find symbol
```

No modo multi-file, a interface pode ativar a aba correspondente e direcionar o usuário para a linha informada.

## 🐳 Docker Health e Execution Guard

O endpoint abaixo verifica se o ambiente de execução está utilizável:

```text
GET /api/v1/health/runner
```

Estados possíveis:

| Estado | Significado |
|---|---|
| `UP` | Docker e imagem do runner disponíveis |
| `DEGRADED` | Docker disponível, mas runner ausente ou incompleto |
| `DOWN` | Docker indisponível |

Enquanto o ambiente não está pronto, o **Execution Guard** bloqueia os botões de execução, mas mantém os editores disponíveis para edição.

## 🔐 Segurança do runner

O código submetido não é executado diretamente na JVM do backend. Cada execução usa um workspace temporário e um container Docker controlado.

Principais restrições:

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

- `/tmp` temporário com restrições;
- diretório temporário dedicado ao Jansi;
- timeout de execução;
- limite de saída;
- limite de tamanho do código-fonte;
- limite de upload ZIP;
- limite de arquivos e bytes extraídos;
- validação de nomes de arquivos Java;
- proteção contra Zip Slip / path traversal;
- descarte do workspace temporário após a execução;
- POM, scripts e testes enviados pelo aluno são ignorados no modo ZIP.

> O sandbox atual foi projetado para uso local e educacional. Uma implantação pública exigiria quotas, filas, isolamento adicional, observabilidade, autenticação e políticas de segurança mais restritivas.

## 🧰 Stack

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 3.5.5 | Backend e API REST |
| Spring Web | Endpoints HTTP |
| Bean Validation | Validação de payloads |
| Spring Data JPA | Persistência |
| Hibernate | ORM |
| JUnit 5 | Testes automatizados |
| Mockito 5.15.2 | Mocks e isolamento de dependências |
| JaCoCo 0.8.12 | Cobertura de código |
| Maven Surefire 3.5.2 | Execução dos testes |
| PostgreSQL 17 | Banco relacional opcional |
| H2 | Banco padrão de desenvolvimento |
| Docker | Sandbox de execução |
| Docker Compose | Infraestrutura local |
| HTML / CSS / JavaScript | Interface web |

## 🚀 Como executar

### Pré-requisitos

- Java 21;
- Maven 3.9+;
- Docker Desktop;
- Git;
- PowerShell no Windows.

### Inicialização rápida

Na raiz do projeto:

```powershell
.\start.ps1
```

O script verifica o Docker, prepara a imagem do runner, executa os testes do projeto e inicia o Spring Boot.

Depois acesse:

```text
http://localhost:8080
```

### Execução manual

```powershell
mvn test
mvn spring-boot:run
```

## 🗄 PostgreSQL

Para subir o PostgreSQL local:

```powershell
docker compose up -d postgres
```

Depois inicie a aplicação com o profile correspondente:

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

Também podem ser utilizadas as variáveis:

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

### Exemplo simplificado de payload multi-file

```json
{
  "className": "PedidoService",
  "sourceFiles": [
    {
      "fileName": "PedidoService.java",
      "content": "public class PedidoService { ... }"
    },
    {
      "fileName": "EstoqueRepository.java",
      "content": "public interface EstoqueRepository { ... }"
    }
  ],
  "testCode": "class PedidoServiceTest { ... }",
  "executionProfile": "JUNIT5_MOCKITO"
}
```

## 👨‍🏫 Modo Professor / Aluno

### Professor

O professor pode cadastrar:

- título;
- descrição;
- classe esperada;
- teste JUnit oculto.

O teste oculto é persistido no backend e não é retornado pela API pública de consulta dos exercícios.

### Aluno — código-fonte

O aluno seleciona o exercício, informa seu nome e envia a implementação Java diretamente pelo editor.

### Aluno — projeto ZIP

Estrutura típica:

```text
meu-projeto.zip
└── src
    └── main
        └── java
            └── Calculadora.java
```

Por segurança, o CodeTest Lab utiliza seu próprio POM e seus próprios testes controlados.

## 📁 Estrutura do projeto

```text
codetest-lab/
├── runner/                     # imagem Docker do executor
├── src/main/java/              # backend Spring Boot
├── src/main/resources/static/  # interface web
├── src/test/java/              # testes automatizados do projeto
├── tools/                      # smoke tests da interface
├── docs/                       # specs e planos de evolução
├── docker-compose.yml          # PostgreSQL local
├── pom.xml                     # build principal
├── start.ps1                   # inicialização no Windows
└── README.md
```

## ✅ Verificação da v0.4.0

Última verificação registrada no Windows em **17/09/2026**:

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
UI_SMOKE_V040_MULTI_FILE_OK
CACHE_BUSTING_V040_OK
```

Comandos utilizados:

```powershell
mvn test

node --check src/main/resources/static/app.js
node --check src/main/resources/static/multi-file-editor.js
node --check src/main/resources/static/runner-health.js

node tools/ui-smoke.mjs
node tools/cache-busting-smoke.mjs
```

Além da suíte automatizada, foi validado manualmente o fluxo completo:

```text
2 arquivos Java
→ sourceFiles[]
→ Spring Boot
→ WorkspaceFactory
→ Docker
→ JUnit 5 + Mockito
→ PASSED
```

## 🧪 Cobertura de testes do próprio projeto

A suíte cobre componentes como:

- validação de `JavaSourceFile`;
- limites e normalização de `ExecutionRequest`;
- contrato HTTP do `PlaygroundController`;
- materialização de múltiplos arquivos no `WorkspaceFactory`;
- construção segura do comando Docker;
- parsing dos resultados Maven/Surefire;
- leitura de JaCoCo XML;
- extração segura de ZIP;
- Docker Health;
- smoke tests da interface;
- regressão de cache-busting;
- regras do serviço de exercícios com Mockito.

## 🛣 Roadmap

- [x] MVP Spring Boot
- [x] execução segura em Docker
- [x] JUnit 5
- [x] Mockito
- [x] JaCoCo e dashboard de cobertura
- [x] modo Professor / Aluno
- [x] submissão por código-fonte
- [x] submissão por ZIP
- [x] histórico de submissões
- [x] detecção de assertion failure
- [x] erro de compilação estruturado
- [x] Docker Health
- [x] Execution Guard
- [x] playground Java multi-file em abas
- [x] cache-busting dos assets da v0.4.0
- [ ] múltiplos arquivos de teste JUnit
- [ ] packages Java e árvore de projeto
- [ ] autenticação e autorização PROFESSOR / ALUNO
- [ ] PostgreSQL com Flyway
- [ ] fila de execução com Kafka
- [ ] observabilidade com métricas e logs
- [ ] IA para explicar falhas de testes
- [ ] turmas, ranking e acompanhamento pedagógico
- [ ] runners para Python e JavaScript

## 🎓 O que este projeto demonstra

Do ponto de vista de portfólio Java Backend, o CodeTest Lab demonstra prática com:

- modelagem de APIs REST;
- validação de contratos de entrada;
- Java Records;
- arquitetura em camadas;
- JPA e persistência relacional;
- JUnit 5 e Mockito;
- cobertura JaCoCo;
- TDD e testes de regressão;
- Docker e isolamento de processos;
- tratamento estruturado de erros;
- segurança no processamento de arquivos ZIP;
- limites de recursos e execução controlada;
- debugging de integração frontend/backend;
- evolução incremental com Git e Pull Requests.

## 👨‍💻 Autor

**Jucelio Farias Coelho**

GitHub: [@juceliocoelho2022](https://github.com/juceliocoelho2022)

---

O CodeTest Lab continua em evolução como projeto de estudo, portfólio e apoio ao ensino de testes de software e desenvolvimento Java Backend.
