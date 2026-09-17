# CodeTest Lab — Multi-File Playground Design

Date: 2026-09-17

## Goal

Evolve the personal playground from a single Java source editor into a small multi-file Java testing workspace that remains simple for students while supporting realistic professional scenarios such as services, repositories, interfaces and Mockito-based tests.

The approved UX is based on **file tabs + `+ Novo arquivo`**, with a hard limit of **10 Java source files per execution**.

## Scope

### Included in v0.4.0

- Multiple Java files in the personal playground.
- File tabs for switching between source files.
- Create, rename and delete source files.
- Maximum of 10 Java source files.
- Maximum aggregate source size of 100,000 characters per execution.
- One existing JUnit test editor remains in place.
- Existing execution profiles remain supported:
  - JUnit 5
  - JUnit 5 + Mockito
  - JUnit 5 + JaCoCo
  - JUnit 5 + Mockito + JaCoCo
- Existing Docker isolation, timeout, health check and Execution Guard remain unchanged.
- Compile-error navigation must switch to the source-file tab that contains the reported error and highlight the reported line.
- Backward compatibility with the current single `sourceCode` request during migration.

### Explicitly out of scope

- Java packages/directories in the editor. Source files are root-level files under `src/main/java` in v0.4.0.
- Multiple JUnit test files. This is planned for a later increment.
- Arbitrary dependency management or user-provided Maven POMs.
- Project tree/sidebar. Tabs are sufficient for this version.
- Importing a local directory or ZIP into the personal multi-file editor.
- Persistence of editor state across browser sessions.

## Current state

The personal playground currently sends one `className`, one `sourceCode`, one `testCode` and one `executionProfile` to `POST /api/v1/playground/run`.

`ExecutionRequest` currently represents exactly one source origin: either `sourceCode` or `zipBytes`. `WorkspaceFactory` writes that single source to `src/main/java/<ClassName>.java`, writes one JUnit test class to `src/test/java`, creates the controlled POM and then executes the workspace through the existing Docker sandbox.

The Professor/Aluno flow already supports project ZIP submissions and is not part of this change.

## Recommended architecture

### 1. Source file model

Introduce a small source-file value object for the execution layer:

```text
JavaSourceFile
- fileName: String
- content: String
```

Rules:

- `fileName` must end in `.java`.
- The base filename must be a valid Java identifier.
- Directory separators are forbidden in v0.4.0.
- Absolute paths, `..`, `.` path segments and encoded path traversal are rejected.
- Duplicate filenames are rejected case-insensitively.
- Empty content is rejected.
- At most 10 files are accepted.
- Aggregate source content may not exceed 100,000 characters.

The existing `className` remains the primary-class identifier. A file named `<simpleClassName(className)>.java` must be present in the effective source-file set.

### 2. API contract

The existing endpoint remains:

```text
POST /api/v1/playground/run
```

New request shape:

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

During the migration, the request may contain either:

- legacy `sourceCode`, or
- new `sourceFiles`.

It must not contain both. Exactly one source form is required.

The controller converts legacy `sourceCode` into a one-element `sourceFiles` collection internally. This preserves API compatibility while allowing the execution pipeline to converge on one multi-file representation.

`className`, `testCode` and `executionProfile` retain their existing meaning.

### 3. ExecutionRequest migration

`ExecutionRequest` should evolve from `sourceCode` to a normalized list of Java source files.

Recommended internal modes:

```text
SOURCE_FILES
ZIP
```

The execution layer should not need to care whether the HTTP request originally used legacy `sourceCode`; normalization happens before workspace creation.

`ExecutionRequest.source(...)` compatibility factories may remain temporarily but should build a one-element source-file list.

ZIP behavior remains unchanged.

### 4. Workspace creation

For source-file executions, `WorkspaceFactory` writes each validated file to:

```text
src/main/java/<fileName>
```

Example:

```text
src/main/java/
├── PedidoService.java
├── EstoqueRepository.java
└── Pedido.java

src/test/java/
└── PedidoServiceTest.java
```

The controlled POM, execution profile handling, Docker command and sandbox configuration remain unchanged.

The workspace factory must revalidate filenames even if DTO validation already ran. Filesystem safety is enforced at the execution boundary, not trusted to the web layer alone.

### 5. Personal playground UI

The Java source side becomes a tabbed workspace.

Initial state:

```text
CÓDIGO JAVA

[ Calculadora.java ] [ + Novo arquivo ]

┌────┬────────────────────────────────────────────┐
│ 1  │ public class Calculadora {                │
│ 2  │     ...                                    │
│ 3  │ }                                          │
└────┴────────────────────────────────────────────┘
```

Professional example:

```text
[ PedidoService.java ] [ EstoqueRepository.java ] [ Pedido.java ] [ + ]
```

Only one Java source editor is visible at a time. Switching tabs swaps the editor content in memory. This avoids stacking multiple editors and preserves the viewport layout already stabilized in v0.3.x.

Each tab supports:

- select;
- rename;
- delete, except when it is the only remaining source file.

`+ Novo arquivo` creates a new file with a safe default name such as `NovaClasse.java`, adding a numeric suffix when necessary.

The UI must prevent creating file 11 and show a concise message explaining the 10-file limit.

### 6. Primary class behavior

The existing `Nome da classe` input becomes `Classe principal`.

It continues to determine the primary class used by diagnostics and compatibility behavior.

The UI keeps the primary filename synchronized with the class name when the primary tab is renamed through the dedicated rename action. Directly editing Java source code does not attempt to parse and auto-rename the file.

Before execution, the client verifies that `<ClassePrincipal>.java` exists. The server performs the same validation authoritatively.

### 7. Diagnostic navigation

Compilation errors already expose a `.java` filename and line number. Multi-file mode extends the current jump-to-line behavior:

1. parse the reported filename;
2. find the matching source tab;
3. activate that tab;
4. render its line numbers;
5. highlight the reported line;
6. scroll the editor to that line.

Errors in the JUnit file continue to jump to the test editor.

If the filename cannot be matched, the result still shows the raw diagnostic without guessing a source tab.

### 8. Result and coverage behavior

No result-contract change is required for v0.4.0.

Existing statuses remain:

- `PASSED`
- `FAILED`
- `COMPILE_ERROR`
- `TIMEOUT`
- `INFRASTRUCTURE_ERROR`

JaCoCo continues to report aggregate coverage for compiled application classes. Per-file coverage visualization is out of scope for this version.

### 9. Execution Guard integration

The v0.3.5 Execution Guard remains authoritative for whether execution buttons are enabled.

Multi-file editing remains available when Docker is unavailable. Only execution is blocked. This lets students continue writing code while the environment is recovering.

### 10. Security and validation

The multi-file API must not weaken the current sandbox model.

Required controls:

- maximum 10 Java source files;
- maximum 100,000 aggregate source characters;
- only `.java` filenames;
- valid Java identifier filename base;
- no slash or backslash in filenames;
- no `..` traversal;
- no absolute paths;
- no duplicate filenames, case-insensitive;
- controlled POM only;
- Docker network disabled;
- existing memory, CPU, PID, capability and privilege restrictions unchanged;
- existing timeout behavior unchanged;
- temporary workspace cleanup unchanged.

Client-side checks are UX only. Server-side validation is mandatory.

## Data flow

```text
Browser file tabs
  -> collect sourceFiles[]
  -> POST /api/v1/playground/run
      className
      sourceFiles[]
      testCode
      executionProfile
  -> PlaygroundRunRequest validation
  -> normalize legacy sourceCode if needed
  -> ExecutionRequest with JavaSourceFile list
  -> WorkspaceFactory
      src/main/java/*.java
      src/test/java/<Test>.java
      controlled pom.xml
  -> Docker sandbox
  -> Maven/Surefire (+ Mockito / JaCoCo per profile)
  -> ExecutionResult
  -> result dashboard
  -> diagnostic filename maps back to source tab
```

## Error handling

### Request validation errors

Return a 400-style API error for:

- more than 10 files;
- duplicate filenames;
- invalid filename;
- missing primary file;
- both `sourceCode` and `sourceFiles` supplied;
- neither supplied;
- aggregate source size above the limit.

The UI should display these as user-actionable validation messages, not `INFRASTRUCTURE_ERROR`.

### Compilation errors

Remain `COMPILE_ERROR`. The UI uses filename + line to select the correct source tab.

### Docker/runner errors

Remain `INFRASTRUCTURE_ERROR` and continue to integrate with Docker Health / Execution Guard.

## Testing strategy

Implementation follows TDD.

### Domain/request tests

Cover:

- one source file;
- multiple source files;
- legacy `sourceCode` normalization;
- reject sourceCode + sourceFiles together;
- reject zero source forms;
- reject more than 10 files;
- reject duplicate filenames;
- reject traversal/path separators;
- reject invalid extension;
- reject aggregate source size overflow;
- require primary source file.

### Workspace tests

Verify that multiple source files are written with exact filenames and contents and that no file can escape `src/main/java`.

### Controller tests

Verify multi-file JSON reaches the executor unchanged after normalization and that legacy requests remain supported.

### Frontend smoke tests

Verify:

- source-file tab bar exists;
- `+ Novo arquivo` exists;
- create/switch/rename/delete behavior is wired;
- 10-file limit is enforced in the UI;
- request body sends `sourceFiles`;
- diagnostic filename activates the matching tab;
- existing execution profile, Docker Health and Execution Guard controls remain present.

### Manual acceptance scenarios

1. Single-file `Calculadora` still passes.
2. `PedidoService.java` + `EstoqueRepository.java` + Mockito test passes.
3. Compile error in `EstoqueRepository.java` activates that tab and highlights the line.
4. JUnit + JaCoCo still displays coverage with multiple application files.
5. Docker DOWN still blocks execution without preventing file editing.

## Compatibility and migration

The legacy single-file API remains accepted in v0.4.0. The browser UI switches to `sourceFiles` immediately.

No migration is required for Professor/Aluno submissions, stored exercises or submission history.

The legacy `sourceCode` field may be removed only in a later major API cleanup after the multi-file flow has been stable.

## Success criteria

v0.4.0 is complete when:

- a user can create up to 10 Java source files in the personal playground;
- files are edited through tabs without increasing page height;
- multi-file source reaches the Docker workspace safely;
- JUnit 5 and Mockito can test dependencies split across files;
- JaCoCo still works with multi-file source;
- compile diagnostics navigate to the correct source file and line;
- the legacy single-file API remains functional;
- all existing regression tests continue to pass.
