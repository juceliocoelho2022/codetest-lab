# CodeTest Lab — Execution Profiles and Result Dashboard Design

Date: 2026-09-15

## Goal

Evolve the CodeTest Lab from a Java/JUnit executor into a configurable testing laboratory with execution profiles and a clearer diagnostic result dashboard.

This increment combines two approved changes:

1. **Result dashboard v2.2** — make PASSED, FAILED and COMPILE_ERROR results easier to understand, with status-specific presentation and technical logs collapsed by default.
2. **Execution profiles v0.3** — let the user choose among supported combinations of JUnit 5, Mockito and JaCoCo without treating those tools as mutually exclusive alternatives.

## Scope

Initial profiles:

- `JUNIT5`
- `JUNIT5_MOCKITO`
- `JUNIT5_JACOCO`
- `JUNIT5_MOCKITO_JACOCO`

JUnit 5 remains the test engine in every initial profile. Mockito and JaCoCo are optional capabilities layered on top of JUnit 5.

Future tools such as AssertJ, Spring Boot Test, MockMvc and Testcontainers are explicitly out of scope for this increment, but the model must allow adding them later without redesigning the API.

## Current state

The runner workspace is created by `WorkspaceFactory`, which writes a controlled Maven `pom.xml` containing JUnit 5 and Mockito for every execution. The Docker runner executes Maven offline through Surefire. `ExecutionResult` currently returns status, test counters, duration and raw output.

This design preserves the current isolation model: submitted code never provides its own effective POM, and the CodeTest Lab continues to control dependencies and plugins.

## Recommended architecture

### 1. Execution profile model

Introduce an `ExecutionProfile` enum in the execution package:

- `JUNIT5(false, false)`
- `JUNIT5_MOCKITO(true, false)`
- `JUNIT5_JACOCO(false, true)`
- `JUNIT5_MOCKITO_JACOCO(true, true)`

The enum exposes capability methods such as:

- `mockitoEnabled()`
- `jacocoEnabled()`

The API accepts the profile name rather than separate booleans. Internally, the enum converts the preset into independent capabilities. This keeps the UI simple while avoiding a rigid implementation.

### 2. Request flow

The personal playground request gains an optional `executionProfile` field.

Default for backward compatibility: `JUNIT5_MOCKITO`.

Flow:

```text
Browser
  -> POST /api/v1/playground/run
      className
      sourceCode
      testCode
      executionProfile
  -> controller / request validation
  -> ExecutionRequest
  -> WorkspaceFactory
  -> controlled Maven POM generated for selected profile
  -> Docker sandbox
  -> Maven/Surefire (+ JaCoCo when enabled)
  -> report readers
  -> ExecutionResult
  -> result dashboard
```

The Professor/Aluno flow keeps its current behavior in the first iteration and uses the default profile unless profile selection is later added to exercises. This limits scope and reduces migration risk.

### 3. Controlled Maven generation

`WorkspaceFactory` must stop returning one fixed POM and instead generate a POM from the selected profile.

All profiles include:

- Java 21 compiler settings
- JUnit Jupiter
- Maven Surefire

Mockito profiles additionally include:

- `mockito-junit-jupiter`

JaCoCo profiles additionally include:

- JaCoCo Maven plugin
- `prepare-agent` before tests
- XML report generation after tests

The generated workspace must remain controlled by the application. Submitted ZIPs must continue to have their own POM, scripts and tests ignored.

### 4. Offline Docker runner

Because runtime containers execute Maven with `-o`, every dependency and plugin required by every supported profile must be preloaded when building `codetest-lab-runner`.

The runner image build must warm:

- JUnit provider / Surefire
- Mockito artifacts
- JaCoCo agent and report plugin

The runtime security controls remain unchanged:

- no network
- read-only container filesystem where currently enforced
- CPU, memory and PID limits
- no-new-privileges
- capability drop
- timeout
- controlled temporary directories

### 5. Coverage result model

Add a nullable coverage object to `ExecutionResult`.

Suggested shape:

```text
CoverageResult
  linePercent
  methodPercent
  branchPercent
  classPercent
```

When JaCoCo is disabled, `coverage` is `null`.

When JaCoCo is enabled and tests complete far enough to produce a report, coverage is populated.

When compilation fails, coverage is `null`.

A missing JaCoCo report after an otherwise valid JaCoCo execution is treated as an infrastructure/reporting problem only if the test execution itself cannot be classified correctly; otherwise the test status remains authoritative and coverage is simply unavailable with a diagnostic note in the technical output.

### 6. JaCoCo parser

Add a focused `JacocoReportReader` that reads `target/site/jacoco/jacoco.xml`.

It must calculate percentages from JaCoCo counters using:

```text
covered / (covered + missed) * 100
```

Counters used:

- LINE
- METHOD
- BRANCH
- CLASS

Rules:

- if denominator is zero, return `null` for that metric rather than inventing 0% or 100%;
- parsing failures must not overwrite an already-known compilation/test status;
- XML parsing must use secure parser settings and never resolve external entities.

### 7. Result dashboard v2.2

The result panel becomes state-specific instead of always presenting the same metrics.

#### PASSED

Show:

- green success accent
- `TODOS OS TESTES PASSARAM`
- human-readable duration (`842 ms`, `8,8 s`, etc.)
- executed / passed / failed / skipped counts when meaningful
- coverage section when JaCoCo is enabled and data is available
- no `(sem saída)` placeholder
- technical log collapsed by default and omitted when there is no technical output worth showing

#### FAILED

Show:

- red failure accent
- `TESTE FALHOU` or `TESTES FALHARAM`
- human-readable duration
- expected / actual values when parsable
- failing test and line when parsable
- test counters
- coverage only if a valid JaCoCo report exists
- technical log under `Ver detalhes técnicos`

#### COMPILE_ERROR

Show:

- warning/error accent
- `ERRO DE COMPILAÇÃO`
- duration
- filename, line, column and compiler message when parsable
- a short guidance message such as `Verifique a instrução próxima à linha N.`
- **do not show `0/0 testes`** because tests never ran
- no coverage section
- technical log collapsed by default

#### TIMEOUT / INFRASTRUCTURE_ERROR

Show concise diagnostic status, duration and technical details. Test counters appear only if they contain meaningful data.

### 8. Profile selector UI

Add a selector near the class/run controls:

```text
Perfil de execução
[ JUnit 5 + Mockito ▼ ]
```

Options:

- JUnit 5
- JUnit 5 + Mockito
- JUnit 5 + JaCoCo
- JUnit 5 + Mockito + JaCoCo

Each option should include a short explanation in accessible helper text or the surrounding UI:

- JUnit 5: execute tests
- Mockito: enable mocks for dependencies
- JaCoCo: collect coverage

The selected profile is sent in the playground request. The initial default is `JUnit 5 + Mockito` to preserve current behavior.

### 9. Coverage visualization

When coverage exists, the result panel displays a compact section:

```text
Cobertura JaCoCo
Linhas    83%
Métodos   100%
Branches  50%
Classes   100%
```

Each metric may use a progress bar plus numeric value. The UI must not imply that a high coverage percentage means the tests are correct; coverage is reported as measurement only.

### 10. Error handling and compatibility

- Missing profile in old requests -> use `JUNIT5_MOCKITO`.
- Unknown profile -> HTTP 400 with a clear validation message.
- Mockito imports under a profile without Mockito -> compilation failure, correctly surfaced as `COMPILE_ERROR`.
- JaCoCo disabled -> no coverage section and no JaCoCo parsing attempt.
- JaCoCo enabled but compilation fails -> `COMPILE_ERROR`, coverage null.
- Test assertion failure -> `FAILED`; coverage may still be available if the report was generated.
- Existing Professor/Aluno API and persistence schema remain unchanged in this increment.

## Testing strategy

Follow TDD for all production changes.

### Backend unit tests

Add or extend tests for:

- execution profile capability mapping;
- workspace POM with and without Mockito;
- workspace POM with and without JaCoCo;
- backward-compatible default profile;
- invalid profile handling;
- JaCoCo XML percentage calculation;
- zero-denominator coverage metrics;
- secure XML parser behavior;
- result composition with and without coverage.

### Runner tests

Validate that the rebuilt runner can execute offline for all four profiles:

1. JUnit 5 only
2. JUnit 5 + Mockito
3. JUnit 5 + JaCoCo
4. JUnit 5 + Mockito + JaCoCo

At least one JaCoCo scenario must prove that `jacoco.xml` is generated inside the workspace.

### Frontend smoke tests

Extend `tools/ui-smoke.mjs` to verify:

- profile selector exists with four options;
- selected profile is included in the request body;
- duration formatter exists;
- compile errors do not render test-count metrics;
- passed/failed/compile result variants exist;
- coverage section is conditional;
- technical details remain collapsible;
- Light/Dark behavior and desktop no-scroll behavior remain intact.

### Manual acceptance scenarios

1. Correct Calculadora with JUnit 5 -> PASSED.
2. Deliberately wrong assertion -> FAILED with expected/actual.
3. Missing semicolon -> COMPILE_ERROR without `0/0 testes`.
4. Mockito test under JUNIT5_MOCKITO -> PASSED.
5. Same Mockito test under JUNIT5 -> COMPILE_ERROR due missing Mockito dependency.
6. JaCoCo profile -> coverage percentages shown.
7. Refresh browser -> theme remains persisted.

## Files expected to change

Likely files:

- `src/main/resources/static/index.html`
- `src/main/resources/static/styles.css`
- `src/main/resources/static/app.js`
- `src/main/java/br/com/codetestlab/execution/ExecutionRequest.java`
- `src/main/java/br/com/codetestlab/execution/ExecutionResult.java`
- `src/main/java/br/com/codetestlab/execution/WorkspaceFactory.java`
- `src/main/java/br/com/codetestlab/execution/DockerCodeExecutor.java`
- playground web request/controller DTO files
- new `ExecutionProfile.java`
- new `CoverageResult.java`
- new `JacocoReportReader.java`
- `runner/pom.xml`
- `runner/Dockerfile`
- corresponding unit tests
- `tools/ui-smoke.mjs`

## Non-goals

Not included in this increment:

- JUnit 4
- TestNG
- Python/JavaScript runners
- user-supplied Maven dependencies
- arbitrary Maven profiles
- arbitrary plugins selected by the user
- coverage thresholds that automatically fail a submission
- changing the Professor/Aluno database schema

## Success criteria

The increment is accepted when:

1. all three core statuses remain correctly classified: PASSED, FAILED and COMPILE_ERROR;
2. the result panel presents status-specific information and never shows meaningless `0/0 testes` for compilation errors;
3. all four execution profiles are selectable and honored by the backend;
4. Mockito is available only for profiles that enable it;
5. JaCoCo profiles generate and return line, method, branch and class coverage when a report is available;
6. runtime execution remains offline and sandboxed;
7. existing requests without `executionProfile` continue to work using JUnit 5 + Mockito;
8. automated tests and UI smoke tests protect the new behavior.
