# CodeTest Lab MVP Design

## Goal
Build a Java 21 web application that serves two use cases: a personal playground for running Java/JUnit tests and a teacher workflow where exercises contain hidden tests and students submit code either as pasted source or a ZIP project.

## Scope
The MVP supports Java 21 only. It accepts pasted source code and ZIP uploads, executes tests inside an isolated Docker runner, stores exercises and submission summaries, and exposes a lightweight browser UI. Authentication, multi-language execution, AI diagnostics, JaCoCo dashboards, and production-grade multi-tenant isolation are intentionally deferred.

## Architecture
- Spring Boot 3.5.5 REST application.
- Static HTML/CSS/JavaScript UI served by Spring Boot.
- JPA persistence with H2 by default and an optional PostgreSQL profile.
- `CodeExecutor` boundary isolates execution concerns from HTTP and persistence.
- `DockerCodeExecutor` materializes a temporary Maven workspace and invokes a pre-built Docker image through `ProcessBuilder`; no shell is used.
- The runner executes with networking disabled, dropped Linux capabilities, `no-new-privileges`, CPU/memory/PID limits, and a hard timeout.
- ZIP submissions are extracted with Zip Slip protection. Only source files below `src/main/java` are copied into a normalized runner workspace; the submitted POM and scripts are ignored.

## Main Flows
### Personal playground
1. User provides class name, Java source, and JUnit test source.
2. API validates size and required fields.
3. Executor creates a temporary normalized Maven project.
4. Docker runner executes `mvn -o -q test`.
5. API returns status, counts when detectable, duration, and bounded output.

### Teacher exercise
1. Teacher creates an exercise with title, description, expected class name, and hidden JUnit test source.
2. Student submits either pasted Java source or a ZIP containing `src/main/java`.
3. The hidden test is injected into the normalized workspace.
4. Docker runner executes tests.
5. Submission result is persisted and shown in the professor dashboard.

## API
- `GET /api/v1/health`
- `POST /api/v1/playground/run`
- `POST /api/v1/exercises`
- `GET /api/v1/exercises`
- `GET /api/v1/exercises/{id}`
- `POST /api/v1/exercises/{id}/submissions/source`
- `POST /api/v1/exercises/{id}/submissions/zip` (multipart)
- `GET /api/v1/exercises/{id}/submissions`

## Data Model
### Exercise
`id`, `title`, `description`, `className`, `hiddenTestCode`, `createdAt`.

### Submission
`id`, `exerciseId`, `studentName`, `submissionType`, `status`, `testsRun`, `testsPassed`, `testsFailed`, `durationMs`, `output`, `createdAt`.

## Error Handling
- Invalid inputs return HTTP 400 with a stable JSON error body.
- Missing exercises return HTTP 404.
- Docker unavailable/build image missing returns a failed execution result rather than crashing the application.
- Execution timeout returns `TIMEOUT`.
- Output is truncated to a configured maximum.
- Temporary workspaces are deleted in `finally` blocks.

## Security Constraints
- Never execute user code in the Spring Boot JVM.
- Never invoke a shell with user-provided strings.
- Docker runtime: `--network none`, `--cap-drop ALL`, `--security-opt no-new-privileges`, memory/CPU/PID limits, timeout.
- ZIP extraction rejects absolute paths, traversal, symlinks by normalization, oversized archives, and excessive extracted bytes.
- ZIP project POMs/scripts are ignored; only `src/main/java/**/*.java` is accepted.

## Testing
- JUnit 5 tests for command construction, output parsing, and ZIP safety.
- Mockito service test for exercise persistence/execution coordination.
- MockMvc controller tests can be added in the next iteration.
- Because this delivery environment has Java but no Maven/Docker/network, a dependency-free Java smoke harness validates core pure-Java components locally; full Maven test execution is documented for the user's machine.

## Future Increments
1. JaCoCo coverage import and visual dashboard.
2. Authentication and teacher/student roles.
3. PostgreSQL migrations with Flyway.
4. Execution queue with Kafka.
5. AI explanation of failing tests.
6. Python/JavaScript runners behind the same `CodeExecutor` contract.
