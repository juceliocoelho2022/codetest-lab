# Docker Health + Infrastructure Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add execution-environment health visibility and correctly distinguish Docker/runner failures from Java test failures.

**Architecture:** Add a small execution-layer health service that probes Docker and the configured runner image with bounded `ProcessBuilder` commands. Expose it through the existing health controller, classify known Docker failures in `ExecutionOutputParser`, and render a compact status pill in the static frontend.

**Tech Stack:** Java 21, Spring Boot 3.5.5, JUnit 5, Maven, Docker CLI, vanilla HTML/CSS/JavaScript.

**Spec:** `docs/superpowers/specs/2026-09-16-docker-health-design.md`

## Global Constraints
- Preserve existing `/api/v1/health` behavior.
- Use argument-list `ProcessBuilder`; no shell interpolation.
- Health probes use a 4-second timeout per command.
- Do not disable test execution solely from the health indicator.
- Keep current Docker sandbox flags unchanged.
- Implement behavior changes test-first.

---

### Task 1: Classify Docker failures correctly

**Files:**
- Modify: `src/test/java/br/com/codetestlab/execution/ExecutionOutputParserTest.java`
- Modify: `src/main/java/br/com/codetestlab/execution/ExecutionOutputParser.java`

**Interfaces:**
- Produces: `ExecutionStatus.INFRASTRUCTURE_ERROR` for known Docker daemon/runner failures.

- [ ] Add failing parser tests for Docker Desktop daemon unavailable and missing runner image.
- [ ] Run focused test and confirm RED.
- [ ] Add minimal infrastructure-failure detection ahead of generic `FAILED` classification.
- [ ] Run focused test and confirm GREEN.

### Task 2: Add runner health service and endpoint

**Files:**
- Create: `src/main/java/br/com/codetestlab/execution/RunnerHealth.java`
- Create: `src/main/java/br/com/codetestlab/execution/DockerHealthService.java`
- Create: `src/test/java/br/com/codetestlab/execution/DockerHealthServiceTest.java`
- Modify: `src/main/java/br/com/codetestlab/web/HealthController.java`
- Create: `src/test/java/br/com/codetestlab/web/HealthControllerTest.java`

**Interfaces:**
- Produces: `DockerHealthService.check()` returning `RunnerHealth`.
- Produces: `GET /api/v1/health/runner`.

- [ ] Write service tests for UP, DEGRADED, DOWN and timeout/error paths.
- [ ] Write controller test for the new endpoint contract.
- [ ] Confirm RED.
- [ ] Implement `RunnerHealth` and `DockerHealthService` with injectable package-private command runner for tests.
- [ ] Inject service into `HealthController` and expose `/runner`.
- [ ] Run focused tests and confirm GREEN.

### Task 3: Add header health indicator

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/styles.css`
- Modify: `tools/ui-smoke.mjs`

**Interfaces:**
- Consumes: `GET /api/v1/health/runner`.
- Produces: clickable `#runnerHealth` status pill with checking/up/degraded/down states.

- [ ] Extend UI smoke test first for the health pill, endpoint call and status classes.
- [ ] Confirm RED.
- [ ] Add header markup and bump badge to `MVP 0.3.4`.
- [ ] Add frontend probe/render logic and manual refresh on click.
- [ ] Add compact responsive styles.
- [ ] Confirm smoke test GREEN and JavaScript syntax check.

### Task 4: Full verification

- [ ] Run `mvn test`.
- [ ] Run `node tools/ui-smoke.mjs`.
- [ ] Start the application and verify `/api/v1/health/runner` with Docker online.
- [ ] Stop Docker Desktop and verify UI/endpoint reports DOWN and execution returns `INFRASTRUCTURE_ERROR`.
- [ ] Restart Docker and confirm health returns UP without rebuilding the application.
