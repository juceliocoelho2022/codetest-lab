# Docker Health + Infrastructure Diagnostics Design

## Goal
Expose the readiness of Docker Desktop and the configured CodeTest Lab runner image, show that state in the UI, and classify Docker/runner failures as infrastructure errors instead of failed tests.

## Scope
- Keep `/api/v1/health` as application health.
- Add `/api/v1/health/runner` for execution-environment health.
- Probe Docker daemon availability first, then the configured runner image.
- Return `UP`, `DEGRADED`, or `DOWN` plus booleans for Docker and runner image readiness.
- Add a compact clickable status pill in the header with a manual refresh action.
- Preserve existing test execution behavior; do not disable the execute button solely because the last health probe failed.
- Classify known Docker daemon/runner failures as `INFRASTRUCTURE_ERROR`.

## Health semantics
- `UP`: Docker daemon responds and configured runner image exists.
- `DEGRADED`: Docker responds but the configured runner image is missing/unavailable.
- `DOWN`: Docker executable cannot start, Docker daemon is unavailable, or the probe times out.

## Security and performance
- Use `ProcessBuilder` argument lists, never shell interpolation.
- Probe timeout: 4 seconds per command.
- Do not expose arbitrary environment data.
- Limit diagnostic messages returned by the health endpoint.

## UX
Header indicator examples:
- `● Docker Online`
- `● Runner ausente`
- `● Docker indisponível`

Clicking the indicator runs the probe again. On page load, it checks once automatically.

Execution-result semantics remain explicit:
- `PASSED`: tests succeeded.
- `FAILED`: tests ran and assertions/errors failed.
- `COMPILE_ERROR`: Java compilation failed.
- `TIMEOUT`: sandbox exceeded execution limit.
- `INFRASTRUCTURE_ERROR`: Docker/runner infrastructure failed.
