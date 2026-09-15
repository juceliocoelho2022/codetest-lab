# Execution Profiles and Result Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver result dashboard v2.2 and configurable JUnit 5 / Mockito / JaCoCo execution profiles while preserving sandboxing and backward compatibility.

**Architecture:** The playground request carries an execution profile preset. `ExecutionProfile` converts that preset into Mockito/JaCoCo capabilities, `WorkspaceFactory` generates a controlled Maven POM, and `DockerCodeExecutor` enriches the normal test result with JaCoCo coverage when requested. The frontend renders state-specific result cards and conditional coverage.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Maven Surefire, JUnit 5.11.4, Mockito 5.15.2, JaCoCo Maven Plugin 0.8.12, Docker, HTML/CSS/JavaScript.

**Spec:** `docs/superpowers/specs/2026-09-15-execution-profiles-and-result-dashboard-design.md`

## Global Constraints

- Keep runtime Docker execution offline and sandboxed.
- JUnit 5 is present in every initial execution profile.
- Default profile for missing values is `JUNIT5_MOCKITO`.
- Professor/Aluno flow keeps current behavior and persistence schema.
- Submitted ZIP POM/scripts/tests remain ignored.
- JaCoCo coverage is measurement only; it does not pass/fail an execution.
- COMPILE_ERROR must not show meaningless `0/0 testes` in the UI.

---

### Task 1: Execution profile and coverage domain model

**Files:**
- Create: `src/main/java/br/com/codetestlab/execution/ExecutionProfile.java`
- Create: `src/main/java/br/com/codetestlab/execution/CoverageResult.java`
- Modify: `src/main/java/br/com/codetestlab/execution/ExecutionRequest.java`
- Modify: `src/main/java/br/com/codetestlab/execution/ExecutionResult.java`
- Test: `src/test/java/br/com/codetestlab/execution/ExecutionProfileTest.java`

- [ ] Write tests for all four profile capability mappings and the default profile.
- [ ] Verify those tests fail before the new model exists.
- [ ] Implement `ExecutionProfile`, optional coverage in `ExecutionResult`, and backward-compatible constructors/factories.
- [ ] Run execution-package tests.
- [ ] Commit.

### Task 2: Playground API profile selection

**Files:**
- Modify: `src/main/java/br/com/codetestlab/web/dto/PlaygroundRunRequest.java`
- Modify: `src/main/java/br/com/codetestlab/web/PlaygroundController.java`
- Test: add controller/request coverage under `src/test/java/br/com/codetestlab/web/` if needed.

- [ ] Write tests for missing/default and invalid profile handling.
- [ ] Verify RED.
- [ ] Add optional `executionProfile` request field and map it to `ExecutionProfile`.
- [ ] Verify GREEN and keep old 3-field JSON requests valid.
- [ ] Commit.

### Task 3: Controlled Maven POM per profile

**Files:**
- Modify: `src/main/java/br/com/codetestlab/execution/WorkspaceFactory.java`
- Test: `src/test/java/br/com/codetestlab/execution/WorkspaceFactoryTest.java`

- [ ] Write tests proving Mockito dependency is present only for Mockito profiles.
- [ ] Write tests proving JaCoCo plugin is present only for JaCoCo profiles.
- [ ] Verify RED.
- [ ] Generate the controlled POM from `ExecutionProfile`.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 4: JaCoCo report parsing and result enrichment

**Files:**
- Create: `src/main/java/br/com/codetestlab/execution/JacocoReportReader.java`
- Modify: `src/main/java/br/com/codetestlab/execution/DockerCodeExecutor.java`
- Modify: `src/main/java/br/com/codetestlab/execution/SurefireReportReader.java`
- Test: `src/test/java/br/com/codetestlab/execution/JacocoReportReaderTest.java`

- [ ] Write XML parsing tests for LINE, METHOD, BRANCH and CLASS counters, including zero denominators.
- [ ] Add a malicious-DOCTYPE parser safety test.
- [ ] Verify RED.
- [ ] Implement secure JaCoCo XML parsing and attach coverage only when JaCoCo is enabled.
- [ ] Preserve the authoritative PASSED/FAILED/COMPILE_ERROR status if coverage parsing is unavailable.
- [ ] Verify GREEN.
- [ ] Commit.

### Task 5: Offline runner cache for every profile

**Files:**
- Modify: `runner/pom.xml`
- Modify: `runner/Dockerfile`

- [ ] Add JaCoCo plugin/version to the controlled runner cache POM.
- [ ] Warm JUnit/Surefire, Mockito and JaCoCo artifacts during runner image build.
- [ ] Rebuild `codetest-lab-runner:latest` on a Docker-capable machine.
- [ ] Run one offline execution for each of the four profiles.
- [ ] Confirm a JaCoCo profile produces `target/site/jacoco/jacoco.xml`.
- [ ] Commit.

### Task 6: Result dashboard v2.2 and profile selector

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/styles.css`
- Modify: `src/main/resources/static/app.js`
- Modify: `tools/ui-smoke.mjs`

- [ ] Extend the smoke test first for four profile options, request payload, human duration, status-specific cards, conditional coverage and hidden `0/0` compile metrics.
- [ ] Verify RED with `node tools/ui-smoke.mjs`.
- [ ] Add the profile selector with default `JUNIT5_MOCKITO`.
- [ ] Render PASSED, FAILED and COMPILE_ERROR as distinct diagnostic cards.
- [ ] Add JaCoCo coverage bars and percentages only when coverage is present.
- [ ] Keep technical logs collapsed under `Ver detalhes técnicos`.
- [ ] Verify `node --check src/main/resources/static/app.js` and `node tools/ui-smoke.mjs`.
- [ ] Commit.

### Task 7: End-to-end verification

**Files:**
- Modify: `README.md` only if documentation is stale.

- [ ] Run `mvn test` and require zero failures/errors.
- [ ] Rebuild the Docker runner.
- [ ] Manually verify PASSED, FAILED and COMPILE_ERROR.
- [ ] Verify Mockito succeeds in `JUNIT5_MOCKITO` and fails to compile in `JUNIT5` when Mockito imports are used.
- [ ] Verify JaCoCo profiles return coverage.
- [ ] Verify Light/Dark persistence and desktop no-scroll still work.
- [ ] Review final diff against the design spec before completion.