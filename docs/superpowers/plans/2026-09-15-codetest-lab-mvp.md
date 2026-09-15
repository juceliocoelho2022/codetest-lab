# CodeTest Lab MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a runnable Java 21/Spring Boot MVP for personal Java testing and teacher-managed hidden-test exercises with source/ZIP submissions executed in a Docker sandbox.

**Architecture:** Keep web/persistence concerns separate from code execution through a `CodeExecutor` interface. Normalize all submissions into a controlled Maven workspace and run that workspace in a hardened Docker runner image, never inside the application JVM.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring Web, Spring Data JPA, Bean Validation, H2, PostgreSQL driver, JUnit 5, Mockito, Maven, Docker.

**Spec:** `docs/superpowers/specs/2026-09-15-codetest-lab-mvp-design.md`

## Global Constraints
- Java 21 only in this MVP.
- Spring Boot 3.5.5.
- Docker execution must use no network, dropped capabilities, no-new-privileges, CPU/memory/PID limits, and timeout.
- User code must never execute inside the Spring Boot JVM.
- ZIP POM/scripts are ignored; only normalized `src/main/java/**/*.java` source is used.
- Default persistence is H2; PostgreSQL is optional through the `postgres` profile.

---

### Task 1: Core execution model and parser
**Files:**
- Create `src/main/java/br/com/codetestlab/execution/ExecutionStatus.java`
- Create `src/main/java/br/com/codetestlab/execution/ExecutionRequest.java`
- Create `src/main/java/br/com/codetestlab/execution/ExecutionResult.java`
- Create `src/main/java/br/com/codetestlab/execution/ExecutionOutputParser.java`
- Test `src/test/java/br/com/codetestlab/execution/ExecutionOutputParserTest.java`

**Interfaces:**
- Produces: immutable execution request/result model and `ExecutionOutputParser.parse(int, String, long)`.

- [ ] Write parser tests for pass, fail, compile failure, and timeout-independent output interpretation.
- [ ] Verify RED with Maven when available; in this environment verify expected missing production symbols with a dependency-free smoke harness.
- [ ] Implement the minimal parser and model.
- [ ] Run local pure-Java smoke checks and full Maven tests when available.

### Task 2: Docker command and workspace safety
**Files:**
- Create `DockerCommandBuilder.java`
- Create `SafeZipExtractor.java`
- Test `DockerCommandBuilderTest.java`
- Test `SafeZipExtractorTest.java`

**Interfaces:**
- Produces: `DockerCommandBuilder.build(Path workspace, String image)` and `SafeZipExtractor.extractJavaSources(InputStream, Path)`.

- [ ] Test security flags and volume mapping.
- [ ] Test valid ZIP extraction and traversal rejection.
- [ ] Implement command builder without shell invocation.
- [ ] Implement bounded safe extraction restricted to `src/main/java/**/*.java`.
- [ ] Run smoke/Maven tests.

### Task 3: Docker executor and runner image
**Files:**
- Create `CodeExecutor.java`
- Create `DockerCodeExecutor.java`
- Create `WorkspaceFactory.java`
- Create `runner/Dockerfile`
- Create `runner/pom.xml`

**Interfaces:**
- Produces: `CodeExecutor.execute(ExecutionRequest)` and a pre-cached `codetest-lab-runner:latest` image.

- [ ] Define executor behavior for source and normalized ZIP inputs.
- [ ] Materialize fixed Maven workspace with source/test files.
- [ ] Run Docker through `ProcessBuilder(List<String>)` with timeout and bounded output.
- [ ] Ensure workspace cleanup in all paths.
- [ ] Document `docker build -t codetest-lab-runner:latest runner`.

### Task 4: Exercise/submission persistence and services
**Files:**
- Create exercise/submission entities, repositories, DTOs, and services.
- Test `ExerciseServiceTest.java` with Mockito.

**Interfaces:**
- Produces CRUD/list operations for exercises and source/ZIP submission execution.

- [ ] Write service tests for exercise creation and source submission coordination.
- [ ] Implement entities/repositories.
- [ ] Implement service validation and persistence.
- [ ] Verify with Maven tests when available.

### Task 5: REST API and browser UI
**Files:**
- Create controllers and exception handler.
- Create `src/main/resources/static/index.html`, `app.js`, `styles.css`.

**Interfaces:**
- Produces the documented REST endpoints and two UI modes: Personal and Professor.

- [ ] Add API DTO validation and error responses.
- [ ] Implement playground and exercise endpoints.
- [ ] Implement source and multipart ZIP submission endpoints.
- [ ] Add UI tabs, forms, result rendering, and exercise/submission refresh.

### Task 6: Configuration, documentation, and packaging
**Files:**
- Create `pom.xml`, application YAMLs, `docker-compose.yml`, `README.md`, and smoke script.

**Interfaces:**
- Produces a project that runs locally with H2 or PostgreSQL and a documented runner build.

- [ ] Configure H2 default and PostgreSQL profile.
- [ ] Add upload/execution limits.
- [ ] Add local smoke validation for pure-Java core.
- [ ] Verify file structure and compile pure-Java core with `javac`.
- [ ] Package a clean ZIP for delivery.
