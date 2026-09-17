# Multi-File Playground Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evolve the personal playground to safely execute up to 10 Java source files through tabs while preserving the current single-file API, execution profiles, diagnostics, Docker sandbox and Execution Guard.

**Architecture:** Normalize HTTP input into a `List<JavaSourceFile>` at the web/execution boundary, keep ZIP execution as a separate mode, and make `WorkspaceFactory` write validated root-level `.java` files into `src/main/java`. The browser manages an in-memory tab model and sends `sourceFiles[]`; compile diagnostics map the reported filename back to the correct tab before jumping to the line.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Jakarta Validation, JUnit 5, Mockito, Maven, vanilla HTML/CSS/JavaScript, Docker sandbox, JaCoCo.

**Spec:** `docs/superpowers/specs/2026-09-17-multi-file-playground-design.md`

## Global Constraints

- Maximum 10 Java source files per personal execution.
- Maximum 100,000 aggregate source characters.
- Only root-level `.java` filenames in v0.4.0; packages/directories are out of scope.
- Filenames must use a valid Java identifier as basename and must not contain `/`, `\\`, `..`, absolute paths or duplicate names case-insensitively.
- The effective source set must contain `<simpleClassName(className)>.java`.
- Legacy `sourceCode` remains accepted, but requests must provide exactly one source form: `sourceCode` or `sourceFiles`.
- One JUnit test editor remains in v0.4.0.
- Existing JUnit/Mockito/JaCoCo profiles, Docker isolation, timeout, Docker Health and Execution Guard behavior remain unchanged.
- Professor/Aluno flow and ZIP submission behavior remain unchanged.
- Implementation follows TDD: every behavior change starts with a failing test and ends with fresh verification.

---

### Task 1: Introduce the validated Java source-file model

**Files:**
- Create: `src/main/java/br/com/codetestlab/execution/JavaSourceFile.java`
- Create: `src/test/java/br/com/codetestlab/execution/JavaSourceFileTest.java`

**Interfaces:**
- Produces: `public record JavaSourceFile(String fileName, String content)` with constructor validation.
- Consumes: no new application types.

- [ ] **Step 1: Write the failing validation tests**

```java
package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaSourceFileTest {

    @Test
    void shouldAcceptSimpleJavaFilename() {
        JavaSourceFile source = new JavaSourceFile(
                "PedidoService.java",
                "public class PedidoService {}");

        assertEquals("PedidoService.java", source.fileName());
    }

    @Test
    void shouldRejectNonJavaExtension() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("PedidoService.txt", "class PedidoService {}"));
    }

    @Test
    void shouldRejectPathSeparatorsAndTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("../PedidoService.java", "class PedidoService {}"));
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("domain/PedidoService.java", "class PedidoService {}"));
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("domain\\PedidoService.java", "class PedidoService {}"));
    }

    @Test
    void shouldRejectInvalidJavaIdentifierFilename() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("pedido-service.java", "class PedidoService {}"));
    }

    @Test
    void shouldRejectBlankContent() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("PedidoService.java", "   "));
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
mvn -q -Dtest=JavaSourceFileTest test
```

Expected: compilation failure because `JavaSourceFile` does not exist.

- [ ] **Step 3: Implement the minimal validated record**

```java
package br.com.codetestlab.execution;

import java.util.regex.Pattern;

public record JavaSourceFile(String fileName, String content) {
    private static final Pattern JAVA_IDENTIFIER =
            Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    public JavaSourceFile {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName é obrigatório.");
        }
        if (fileName.contains("/") || fileName.contains("\\\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Nome de arquivo Java inválido.");
        }
        if (!fileName.endsWith(".java")) {
            throw new IllegalArgumentException("O arquivo deve terminar em .java.");
        }

        String baseName = fileName.substring(0, fileName.length() - ".java".length());
        if (!JAVA_IDENTIFIER.matcher(baseName).matches()) {
            throw new IllegalArgumentException("Nome de arquivo Java inválido.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content é obrigatório.");
        }
    }
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```bash
mvn -q -Dtest=JavaSourceFileTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/codetestlab/execution/JavaSourceFile.java src/test/java/br/com/codetestlab/execution/JavaSourceFileTest.java
git commit -m "feat: add validated Java source file model"
```

---

### Task 2: Normalize `ExecutionRequest` around source-file lists

**Files:**
- Modify: `src/main/java/br/com/codetestlab/execution/ExecutionRequest.java`
- Create or modify: `src/test/java/br/com/codetestlab/execution/ExecutionRequestTest.java`

**Interfaces:**
- Consumes: `JavaSourceFile` from Task 1.
- Produces: `ExecutionRequest.sourceFiles(String className, List<JavaSourceFile> sourceFiles, String testCode, ExecutionProfile profile)` and `List<JavaSourceFile> sourceFiles()`.
- Keeps: compatibility factories `ExecutionRequest.source(...)` and ZIP factories.

- [ ] **Step 1: Write failing tests for normalized source files and compatibility**

```java
package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionRequestTest {

    @Test
    void shouldNormalizeLegacySingleSourceToOneSourceFile() {
        ExecutionRequest request = ExecutionRequest.source(
                "Calculadora",
                "public class Calculadora {}",
                "class CalculadoraTest {}",
                ExecutionProfile.JUNIT5);

        assertEquals(1, request.sourceFiles().size());
        assertEquals("Calculadora.java", request.sourceFiles().getFirst().fileName());
    }

    @Test
    void shouldAcceptMultipleSourceFiles() {
        ExecutionRequest request = ExecutionRequest.sourceFiles(
                "PedidoService",
                List.of(
                        new JavaSourceFile("PedidoService.java", "public class PedidoService {}"),
                        new JavaSourceFile("EstoqueRepository.java", "public interface EstoqueRepository {}")),
                "class PedidoServiceTest {}",
                ExecutionProfile.JUNIT5_MOCKITO);

        assertEquals(2, request.sourceFiles().size());
    }

    @Test
    void shouldRejectMoreThanTenFiles() {
        List<JavaSourceFile> files = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(i -> new JavaSourceFile("Classe" + i + ".java", "class Classe" + i + " {}"))
                .toList();

        assertThrows(IllegalArgumentException.class, () ->
                ExecutionRequest.sourceFiles("Classe1", files, "class Classe1Test {}", ExecutionProfile.JUNIT5));
    }

    @Test
    void shouldRejectDuplicateNamesCaseInsensitively() {
        assertThrows(IllegalArgumentException.class, () ->
                ExecutionRequest.sourceFiles(
                        "PedidoService",
                        List.of(
                                new JavaSourceFile("PedidoService.java", "public class PedidoService {}"),
                                new JavaSourceFile("pedidoservice.java", "class pedidoservice {}")),
                        "class PedidoServiceTest {}",
                        ExecutionProfile.JUNIT5));
    }

    @Test
    void shouldRequirePrimarySourceFile() {
        assertThrows(IllegalArgumentException.class, () ->
                ExecutionRequest.sourceFiles(
                        "PedidoService",
                        List.of(new JavaSourceFile("Outro.java", "class Outro {}")),
                        "class PedidoServiceTest {}",
                        ExecutionProfile.JUNIT5));
    }

    @Test
    void zipRequestShouldRemainZipMode() {
        ExecutionRequest request = ExecutionRequest.zip(
                "Calculadora", new byte[]{1}, "class CalculadoraTest {}");

        assertTrue(request.isZip());
    }
}
```

- [ ] **Step 2: Run and verify RED**

```bash
mvn -q -Dtest=ExecutionRequestTest test
```

Expected: compilation failures for `sourceFiles(...)` / `sourceFiles()`.

- [ ] **Step 3: Refactor `ExecutionRequest` to normalized source files**

Implement these invariants in the record/factories:

```java
private static final int MAX_SOURCE_FILES = 10;
private static final int MAX_SOURCE_CHARS = 100_000;
```

For source mode:

```java
List<JavaSourceFile> copy = List.copyOf(sourceFiles);
if (copy.isEmpty()) throw new IllegalArgumentException("Informe ao menos um arquivo Java.");
if (copy.size() > MAX_SOURCE_FILES) throw new IllegalArgumentException("O limite é de 10 arquivos Java.");

long totalChars = copy.stream().mapToLong(file -> file.content().length()).sum();
if (totalChars > MAX_SOURCE_CHARS) {
    throw new IllegalArgumentException("O código-fonte excede o limite total de 100000 caracteres.");
}

long uniqueNames = copy.stream()
        .map(file -> file.fileName().toLowerCase(java.util.Locale.ROOT))
        .distinct()
        .count();
if (uniqueNames != copy.size()) {
    throw new IllegalArgumentException("Nomes de arquivo Java duplicados não são permitidos.");
}

String primaryFile = WorkspaceFactory.simpleClassName(className) + ".java";
boolean primaryPresent = copy.stream().anyMatch(file -> file.fileName().equals(primaryFile));
if (!primaryPresent) {
    throw new IllegalArgumentException("O arquivo principal " + primaryFile + " é obrigatório.");
}
```

Compatibility factory:

```java
public static ExecutionRequest source(String className,
                                      String sourceCode,
                                      String testCode,
                                      ExecutionProfile profile) {
    String fileName = WorkspaceFactory.simpleClassName(className) + ".java";
    return sourceFiles(
            className,
            List.of(new JavaSourceFile(fileName, sourceCode)),
            testCode,
            profile);
}
```

Keep ZIP defensive copying and `isZip()` behavior unchanged.

- [ ] **Step 4: Run focused tests and verify GREEN**

```bash
mvn -q -Dtest=ExecutionRequestTest test
```

Expected: PASS.

- [ ] **Step 5: Run existing execution request / profile regressions**

```bash
mvn -q -Dtest=ExecutionProfileTest,PlaygroundControllerTest test
```

Expected: PASS after any constructor call sites are updated.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/codetestlab/execution/ExecutionRequest.java src/test/java/br/com/codetestlab/execution/ExecutionRequestTest.java
git commit -m "refactor: normalize execution requests to source files"
```

---

### Task 3: Extend the playground HTTP contract while preserving legacy `sourceCode`

**Files:**
- Create: `src/main/java/br/com/codetestlab/web/dto/JavaSourceFileRequest.java`
- Modify: `src/main/java/br/com/codetestlab/web/dto/PlaygroundRunRequest.java`
- Modify: `src/main/java/br/com/codetestlab/web/PlaygroundController.java`
- Modify: `src/test/java/br/com/codetestlab/web/PlaygroundControllerTest.java`

**Interfaces:**
- Consumes: `JavaSourceFile`, `ExecutionRequest.sourceFiles(...)`.
- Produces HTTP input fields: optional `sourceCode`, optional `List<JavaSourceFileRequest> sourceFiles`.
- Exactly one HTTP source form must be present.

- [ ] **Step 1: Add failing controller tests**

Add these cases to `PlaygroundControllerTest`:

```java
@Test
void shouldForwardMultipleSourceFilesToExecutor() {
    AtomicReference<ExecutionRequest> captured = new AtomicReference<>();
    PlaygroundController controller = new PlaygroundController(request -> {
        captured.set(request);
        return new ExecutionResult(ExecutionStatus.PASSED, 1, 1, 0, 0, 1, "");
    });

    controller.run(new PlaygroundRunRequest(
            "PedidoService",
            null,
            List.of(
                    new JavaSourceFileRequest("PedidoService.java", "public class PedidoService {}"),
                    new JavaSourceFileRequest("EstoqueRepository.java", "public interface EstoqueRepository {}")),
            "class PedidoServiceTest {}",
            "JUNIT5_MOCKITO"));

    assertEquals(2, captured.get().sourceFiles().size());
    assertEquals("EstoqueRepository.java", captured.get().sourceFiles().get(1).fileName());
}

@Test
void shouldKeepLegacySourceCodeRequestWorking() {
    AtomicReference<ExecutionRequest> captured = new AtomicReference<>();
    PlaygroundController controller = new PlaygroundController(request -> {
        captured.set(request);
        return new ExecutionResult(ExecutionStatus.PASSED, 1, 1, 0, 0, 1, "");
    });

    controller.run(new PlaygroundRunRequest(
            "Calculadora",
            "public class Calculadora {}",
            null,
            "class CalculadoraTest {}",
            "JUNIT5"));

    assertEquals(1, captured.get().sourceFiles().size());
    assertEquals("Calculadora.java", captured.get().sourceFiles().getFirst().fileName());
}

@Test
void shouldRejectSourceCodeAndSourceFilesTogether() {
    PlaygroundController controller = new PlaygroundController(request ->
            new ExecutionResult(ExecutionStatus.PASSED, 0, 0, 0, 0, 1, ""));

    PlaygroundRunRequest request = new PlaygroundRunRequest(
            "Calculadora",
            "public class Calculadora {}",
            List.of(new JavaSourceFileRequest("Calculadora.java", "public class Calculadora {}")),
            "class CalculadoraTest {}",
            "JUNIT5");

    assertThrows(IllegalArgumentException.class, () -> controller.run(request));
}
```

- [ ] **Step 2: Run and verify RED**

```bash
mvn -q -Dtest=PlaygroundControllerTest test
```

Expected: compilation failure because the new request DTO/API shape does not exist.

- [ ] **Step 3: Create `JavaSourceFileRequest`**

```java
package br.com.codetestlab.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JavaSourceFileRequest(
        @NotBlank @Size(max = 190) String fileName,
        @NotBlank @Size(max = 100_000) String content
) {}
```

- [ ] **Step 4: Evolve `PlaygroundRunRequest`**

Use this logical shape:

```java
public record PlaygroundRunRequest(
        @NotBlank @Size(max = 180) String className,
        @Size(max = 100_000) String sourceCode,
        @Size(max = 10) List<@Valid JavaSourceFileRequest> sourceFiles,
        @NotBlank @Size(max = 100_000) String testCode,
        @Size(max = 80) String executionProfile
) {
    // keep existing 3-arg compatibility constructor
}
```

Do not attempt to express the “exactly one source form” rule with annotations; enforce it in the controller so the same rule is obvious and unit-testable.

- [ ] **Step 5: Normalize request source form in `PlaygroundController`**

Use explicit branching:

```java
boolean hasLegacySource = request.sourceCode() != null && !request.sourceCode().isBlank();
boolean hasSourceFiles = request.sourceFiles() != null && !request.sourceFiles().isEmpty();
if (hasLegacySource == hasSourceFiles) {
    throw new IllegalArgumentException("Informe exatamente uma origem: sourceCode ou sourceFiles.");
}

ExecutionRequest executionRequest;
if (hasSourceFiles) {
    List<JavaSourceFile> files = request.sourceFiles().stream()
            .map(file -> new JavaSourceFile(file.fileName(), file.content()))
            .toList();
    executionRequest = ExecutionRequest.sourceFiles(
            request.className(), files, request.testCode(), profile);
} else {
    executionRequest = ExecutionRequest.source(
            request.className(), request.sourceCode(), request.testCode(), profile);
}
```

- [ ] **Step 6: Run focused tests and verify GREEN**

```bash
mvn -q -Dtest=PlaygroundControllerTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/br/com/codetestlab/web/dto src/main/java/br/com/codetestlab/web/PlaygroundController.java src/test/java/br/com/codetestlab/web/PlaygroundControllerTest.java
git commit -m "feat: accept multi-file playground requests"
```

---

### Task 4: Write multiple source files safely into the Maven workspace

**Files:**
- Modify: `src/main/java/br/com/codetestlab/execution/WorkspaceFactory.java`
- Modify: `src/test/java/br/com/codetestlab/execution/WorkspaceFactoryTest.java`

**Interfaces:**
- Consumes: normalized `ExecutionRequest.sourceFiles()`.
- Produces: one root-level file under `src/main/java` per validated `JavaSourceFile`.

- [ ] **Step 1: Add failing workspace tests**

Extend `WorkspaceFactoryTest` with `@TempDir`-style assertions by calling `create(...)` and cleaning the returned path in the test:

```java
@Test
void shouldWriteEverySourceFileWithExactNameAndContent() throws Exception {
    ExecutionProperties properties = new ExecutionProperties(
            "codetest-lab-runner:latest", 20, 20_000, 100_000,
            1_048_576, 200, 2_000_000);
    WorkspaceFactory factory = new WorkspaceFactory(properties);

    Path workspace = factory.create(ExecutionRequest.sourceFiles(
            "PedidoService",
            List.of(
                    new JavaSourceFile("PedidoService.java", "public class PedidoService {}"),
                    new JavaSourceFile("EstoqueRepository.java", "public interface EstoqueRepository {}")),
            "class PedidoServiceTest {}",
            ExecutionProfile.JUNIT5_MOCKITO));

    assertEquals(
            "public class PedidoService {}",
            Files.readString(workspace.resolve("src/main/java/PedidoService.java")));
    assertEquals(
            "public interface EstoqueRepository {}",
            Files.readString(workspace.resolve("src/main/java/EstoqueRepository.java")));
}
```

Also add a regression asserting single-source compatibility still writes `Calculadora.java`.

- [ ] **Step 2: Run and verify RED**

```bash
mvn -q -Dtest=WorkspaceFactoryTest test
```

Expected: failure because `WorkspaceFactory` still calls `request.sourceCode()`.

- [ ] **Step 3: Replace the single-source write branch**

Replace:

```java
String sourceFile = simpleClassName(request.className()) + ".java";
Files.writeString(mainRoot.resolve(sourceFile), request.sourceCode(), StandardCharsets.UTF_8);
```

with:

```java
for (JavaSourceFile source : request.sourceFiles()) {
    Path destination = mainRoot.resolve(source.fileName()).normalize();
    if (!destination.getParent().equals(mainRoot)) {
        throw new IllegalArgumentException("Nome de arquivo Java inválido.");
    }
    Files.writeString(destination, source.content(), StandardCharsets.UTF_8);
}
```

Keep ZIP handling and controlled POM generation unchanged.

- [ ] **Step 4: Run focused tests and verify GREEN**

```bash
mvn -q -Dtest=WorkspaceFactoryTest,ExecutionRequestTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/codetestlab/execution/WorkspaceFactory.java src/test/java/br/com/codetestlab/execution/WorkspaceFactoryTest.java
git commit -m "feat: materialize multi-file Java workspaces"
```

---

### Task 5: Add the tabbed source-file workspace to the personal UI

**Files:**
- Modify: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/multi-file-editor.css`
- Create: `src/main/resources/static/multi-file-editor.js`
- Modify: `tools/ui-smoke.mjs`

**Interfaces:**
- Produces browser API `window.codeTestSourceWorkspace` with:
  - `getFiles(): Array<{fileName, content}>`
  - `activateFile(fileName): boolean`
  - `getActiveFileName(): string`
  - `renameActiveFile(newName): void`
  - `createFile(): void`
  - `deleteActiveFile(): void`
- Reuses existing textarea `#personalSource` and gutter `#personalSourceLines` rather than creating multiple editors.

- [ ] **Step 1: Add failing UI smoke assertions**

Add assertions to `tools/ui-smoke.mjs`:

```js
const multiFileCss = fs.existsSync('src/main/resources/static/multi-file-editor.css')
  ? fs.readFileSync('src/main/resources/static/multi-file-editor.css', 'utf8')
  : '';
const multiFileJs = fs.existsSync('src/main/resources/static/multi-file-editor.js')
  ? fs.readFileSync('src/main/resources/static/multi-file-editor.js', 'utf8')
  : '';

assert.match(html, /id="sourceFileTabs"/, 'source file tab bar must exist');
assert.match(html, /id="addSourceFile"/, 'new source file action must exist');
assert.match(html, /multi-file-editor\.css/, 'multi-file editor stylesheet must load');
assert.match(html, /multi-file-editor\.js/, 'multi-file editor script must load');
assert.match(multiFileJs, /MAX_SOURCE_FILES\s*=\s*10/, 'UI must enforce ten source files');
assert.match(multiFileJs, /getFiles\s*:/, 'workspace API must expose getFiles');
assert.match(multiFileJs, /activateFile\s*:/, 'workspace API must expose activateFile');
assert.match(multiFileJs, /renameActiveFile/, 'source files must be renameable');
assert.match(multiFileJs, /deleteActiveFile/, 'source files must be deletable');
assert.match(multiFileCss, /\.source-file-tabs/, 'source file tabs must be styled');
assert.match(html, /MVP 0\.4\.0/, 'MVP badge must identify v0.4.0');
```

- [ ] **Step 2: Run and verify RED**

```bash
node tools/ui-smoke.mjs
```

Expected: FAIL because the tab UI and files do not exist.

- [ ] **Step 3: Add minimal tab controls to `index.html`**

Inside the left `Código Java` editor field, add a tab strip above the existing numbered editor:

```html
<div class="source-file-toolbar">
  <div id="sourceFileTabs" class="source-file-tabs" role="tablist" aria-label="Arquivos Java"></div>
  <div class="source-file-actions">
    <button id="renameSourceFile" class="source-file-action" type="button">Renomear</button>
    <button id="deleteSourceFile" class="source-file-action danger" type="button">Excluir</button>
    <button id="addSourceFile" class="source-file-add" type="button">+ Novo arquivo</button>
  </div>
</div>
```

Load:

```html
<link rel="stylesheet" href="/multi-file-editor.css">
```

and after `app.js` but before `runner-health.js`:

```html
<script src="/multi-file-editor.js"></script>
```

Change visible label `Nome da classe` to `Classe principal`, and change the badge text to `MVP 0.4.0`.

- [ ] **Step 4: Implement the in-memory file model in `multi-file-editor.js`**

Initialize from current single editor content:

```js
const MAX_SOURCE_FILES = 10;
const sourceTextarea = document.getElementById('personalSource');
const classNameInput = document.getElementById('personalClassName');

let files = [{
  fileName: `${classNameInput.value.trim()}.java`,
  content: sourceTextarea.value
}];
let activeIndex = 0;
```

Before switching tabs, copy textarea content into the active item. After switching, put the selected item content into the textarea and dispatch `input` so existing line numbering updates.

`createFile()` must choose `NovaClasse.java`, then `NovaClasse2.java`, etc., and stop at 10 files with a concise `alert('O limite é de 10 arquivos Java.')` for v0.4.0.

`renameActiveFile(newName)` must validate with `/^[A-Za-z_$][A-Za-z\d_$]*\.java$/`, reject case-insensitive duplicates and, when renaming the current primary file, update `#personalClassName` to the new basename.

`deleteActiveFile()` must reject deleting the current primary file and reject deleting the only remaining file.

Expose:

```js
window.codeTestSourceWorkspace = {
  getFiles,
  activateFile,
  getActiveFileName,
  renameActiveFile,
  createFile,
  deleteActiveFile
};
```

Wire rename through `window.prompt('Novo nome do arquivo Java:', currentName)` to keep this version bounded; no modal component is needed.

- [ ] **Step 5: Add compact CSS that preserves editor height**

Use one-row tabs and horizontal overflow:

```css
.source-file-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 5px;
}

.source-file-tabs {
  min-width: 0;
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.source-file-tab {
  flex: 0 0 auto;
  border: 1px solid var(--line);
  border-radius: 7px 7px 0 0;
  background: var(--surface-3);
  color: var(--muted);
  padding: 5px 8px;
  font-size: .64rem;
  font-weight: 750;
}

.source-file-tab.active {
  color: var(--text);
  border-color: var(--accent-2);
  background: var(--surface-2);
}
```

Keep controls compact under the existing short-height media query.

- [ ] **Step 6: Run smoke test and verify GREEN**

```bash
node tools/ui-smoke.mjs
```

Expected: output ending in a v0.4.0 success marker, for example `UI_SMOKE_V040_MULTI_FILE_OK`.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/multi-file-editor.css src/main/resources/static/multi-file-editor.js tools/ui-smoke.mjs
git commit -m "feat: add tabbed Java source workspace"
```

---

### Task 6: Send `sourceFiles[]` from the browser and enforce aggregate limits client-side

**Files:**
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/multi-file-editor.js`
- Modify: `tools/ui-smoke.mjs`

**Interfaces:**
- Consumes: `window.codeTestSourceWorkspace.getFiles()`.
- Produces request body field `sourceFiles` instead of browser-generated `sourceCode`.

- [ ] **Step 1: Add failing smoke assertions for request shape**

```js
assert.match(js, /sourceFiles:\s*window\.codeTestSourceWorkspace\.getFiles\(\)/,
  'personal execution must send sourceFiles');
assert.doesNotMatch(js, /sourceCode:\s*\$\(['"]personalSource['"]\)\.value/,
  'personal browser flow must stop sending legacy sourceCode');
assert.match(multiFileJs, /MAX_SOURCE_CHARS\s*=\s*100_000/,
  'browser workspace must enforce aggregate source size');
```

- [ ] **Step 2: Run and verify RED**

```bash
node tools/ui-smoke.mjs
```

Expected: FAIL on `sourceFiles` request assertion.

- [ ] **Step 3: Add aggregate source validation to workspace API**

In `multi-file-editor.js`:

```js
const MAX_SOURCE_CHARS = 100_000;

function getFiles() {
  persistActiveContent();
  const total = files.reduce((sum, file) => sum + file.content.length, 0);
  if (total > MAX_SOURCE_CHARS) {
    throw new Error('O código-fonte excede o limite total de 100.000 caracteres.');
  }
  return files.map(file => ({...file}));
}
```

Also assert that `${classNameInput.value.trim()}.java` exists before returning files; otherwise throw `O arquivo da classe principal não existe.`.

- [ ] **Step 4: Change `runPersonalTests()` request body**

Replace the legacy field with:

```js
sourceFiles: window.codeTestSourceWorkspace.getFiles(),
```

Keep `className`, `testCode` and `executionProfile` unchanged.

Because `getFiles()` can throw validation errors, move request-body construction inside the existing `try` block so `renderError(...)` shows the message consistently.

- [ ] **Step 5: Run smoke test and syntax check**

```bash
node --check src/main/resources/static/multi-file-editor.js
node --check src/main/resources/static/app.js
node tools/ui-smoke.mjs
```

Expected: all commands exit 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/app.js src/main/resources/static/multi-file-editor.js tools/ui-smoke.mjs
git commit -m "feat: submit personal executions as source files"
```

---

### Task 7: Make compile diagnostics activate the correct Java file tab

**Files:**
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/multi-file-editor.js`
- Modify: `tools/ui-smoke.mjs`

**Interfaces:**
- Consumes: parsed compile filename from `parseExecutionDetails(...)` and `window.codeTestSourceWorkspace.activateFile(fileName)`.
- Produces: correct source tab active before existing line-highlighting logic runs.

- [ ] **Step 1: Add failing smoke assertions**

```js
assert.match(js, /codeTestSourceWorkspace\.activateFile\(details\.file\)/,
  'compile diagnostics must activate the reported Java source file');
assert.match(multiFileJs, /function activateFile\(/,
  'workspace must support file activation by filename');
```

- [ ] **Step 2: Run and verify RED**

```bash
node tools/ui-smoke.mjs
```

Expected: FAIL because diagnostic navigation does not activate source tabs.

- [ ] **Step 3: Make `activateFile(fileName)` case-sensitive and safe**

Implementation:

```js
function activateFile(fileName) {
  const index = files.findIndex(file => file.fileName === fileName);
  if (index < 0) return false;
  switchTo(index);
  return true;
}
```

Do not guess by basename transformations; the compiler filename must match a known tab exactly.

- [ ] **Step 4: Update diagnostic routing in `app.js`**

Before returning `'source'` for `COMPILE_ERROR`, when `details.file` ends with `.java` and is not the test file:

```js
if (window.codeTestSourceWorkspace && details.file) {
  window.codeTestSourceWorkspace.activateFile(details.file);
}
```

Then let the existing `jumpToEditorLine('source', ...)` render/highlight the line in the newly active file.

For JUnit compile errors ending in `Test.java`, keep routing to the test editor.

- [ ] **Step 5: Run smoke + syntax checks**

```bash
node --check src/main/resources/static/multi-file-editor.js
node --check src/main/resources/static/app.js
node tools/ui-smoke.mjs
```

Expected: all exit 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/app.js src/main/resources/static/multi-file-editor.js tools/ui-smoke.mjs
git commit -m "feat: navigate compile errors across source tabs"
```

---

### Task 8: Add backend multi-file integration coverage

**Files:**
- Create: `src/test/java/br/com/codetestlab/execution/MultiFileExecutionTest.java` if existing test patterns allow Docker-free workspace verification; otherwise add cases to `WorkspaceFactoryTest` and `PlaygroundControllerTest` only.
- Modify: `src/test/java/br/com/codetestlab/execution/WorkspaceFactoryTest.java`
- Modify: `src/test/java/br/com/codetestlab/web/PlaygroundControllerTest.java`

**Interfaces:**
- Verifies the complete non-Docker chain: DTO/controller → `ExecutionRequest` → workspace files.

- [ ] **Step 1: Add a realistic Mockito multi-file workspace test**

Create an execution request containing:

```java
new JavaSourceFile("EstoqueRepository.java", """
        public interface EstoqueRepository {
            boolean temEstoque(String produto, int quantidade);
            void retirarEstoque(String produto, int quantidade);
        }
        """),
new JavaSourceFile("PedidoService.java", """
        public class PedidoService {
            private final EstoqueRepository repository;

            public PedidoService(EstoqueRepository repository) {
                this.repository = repository;
            }

            public boolean realizar(String produto, int quantidade) {
                if (!repository.temEstoque(produto, quantidade)) return false;
                repository.retirarEstoque(produto, quantidade);
                return true;
            }
        }
        """)
```

with test code importing Mockito. Assert both main files and the test file are materialized and that `runnerPom(JUNIT5_MOCKITO)` contains Mockito.

- [ ] **Step 2: Run the focused backend suite**

```bash
mvn -q -Dtest=JavaSourceFileTest,ExecutionRequestTest,WorkspaceFactoryTest,PlaygroundControllerTest test
```

Expected: PASS.

- [ ] **Step 3: Run the complete Maven test suite**

```bash
mvn test
```

Expected: `BUILD SUCCESS`, zero test failures/errors.

- [ ] **Step 4: Commit integration-test adjustments**

```bash
git add src/test/java
git commit -m "test: cover multi-file playground integration"
```

---

### Task 9: Manual acceptance on Windows with real Docker runner

**Files:**
- No production code unless a defect is found; any defect starts a new RED-GREEN cycle before modification.

**Interfaces:**
- Validates the user-visible and Docker-integrated success criteria from the spec.

- [ ] **Step 1: Build/confirm runner and start application**

```powershell
cd C:\Projetos\codetest-lab
docker image inspect codetest-lab-runner:latest
mvn test
mvn spring-boot:run
```

Expected: runner image exists, Maven tests pass, application starts on port 8080.

- [ ] **Step 2: Validate single-file regression**

Use `Calculadora.java` + `CalculadoraTest.java` with `JUnit 5`.

Expected: `PASSED`, 1/1, same result dashboard behavior as v0.3.5.

- [ ] **Step 3: Validate professional multi-file Mockito scenario**

Create tabs:

```text
PedidoService.java
EstoqueRepository.java
```

Use `JUnit 5 + Mockito` and this test shape:

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PedidoServiceTest {
    @Test
    void deveRetirarEstoqueQuandoDisponivel() {
        EstoqueRepository repository = mock(EstoqueRepository.class);
        when(repository.temEstoque("Notebook", 1)).thenReturn(true);

        PedidoService service = new PedidoService(repository);

        assertTrue(service.realizar("Notebook", 1));
        verify(repository).retirarEstoque("Notebook", 1);
    }
}
```

Expected: `PASSED`.

- [ ] **Step 4: Validate diagnostic tab navigation**

Introduce a compile error in `EstoqueRepository.java` and execute.

Expected: `COMPILE_ERROR`; `EstoqueRepository.java` becomes active automatically and its reported line is highlighted.

- [ ] **Step 5: Validate JaCoCo with multiple files**

Run the same multi-file workspace using `JUnit 5 + Mockito + JaCoCo`.

Expected: test result plus aggregate JaCoCo coverage visible in the existing coverage panel.

- [ ] **Step 6: Validate Execution Guard remains independent from editing**

Stop Docker Desktop, click health refresh.

Expected: execution becomes blocked, guard is visible, but source tabs can still be created, switched and edited. Restart Docker and verify execution unlocks after refresh.

---

### Task 10: Final regression, version verification and documentation

**Files:**
- Modify: `README.md` only if current README documents personal playground capabilities/version.
- Modify: `tools/ui-smoke.mjs` final success marker to `UI_SMOKE_V040_MULTI_FILE_OK`.

**Interfaces:**
- No new runtime interfaces.

- [ ] **Step 1: Run all automated verification fresh**

```bash
mvn test
node --check src/main/resources/static/app.js
node --check src/main/resources/static/multi-file-editor.js
node --check src/main/resources/static/runner-health.js
node tools/ui-smoke.mjs
```

Expected:

```text
BUILD SUCCESS
UI_SMOKE_V040_MULTI_FILE_OK
```

and all Node syntax checks exit 0.

- [ ] **Step 2: Check changed-file scope**

```bash
git status --short
git diff --stat HEAD~1
```

Confirm no unrelated Professor/Aluno, Docker security, runner POM or persistence changes were introduced.

- [ ] **Step 3: Update README with the final user-facing capability**

Document only verified behavior:

```text
- Personal playground with up to 10 Java source files in tabs
- Single JUnit test editor
- JUnit 5 / Mockito / JaCoCo execution profiles
- Compile-error navigation across source files
- Docker Health + Execution Guard
```

- [ ] **Step 4: Commit final documentation/version cleanup**

```bash
git add README.md tools/ui-smoke.mjs
git commit -m "docs: document multi-file playground v0.4.0"
```

- [ ] **Step 5: Final evidence before completion claim**

Run again after the final commit:

```bash
mvn test
node tools/ui-smoke.mjs
```

Do not claim v0.4.0 complete unless both are green and the Windows Docker acceptance scenarios from Task 9 have been observed.
