package br.com.codetestlab.execution;

import br.com.codetestlab.config.ExecutionProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerHealthServiceTest {

    private final ExecutionProperties properties = new ExecutionProperties(
            "codetest-lab-runner:latest", 20, 20_000, 100_000,
            1_048_576, 200, 2_000_000);

    @Test
    void shouldReportUpWhenDockerAndRunnerImageAreAvailable() {
        DockerHealthService service = new DockerHealthService(properties, (command, timeout) -> {
            assertEquals(Duration.ofSeconds(4), timeout);
            if (command.contains("info")) {
                return new DockerHealthService.CommandResult(0, "29.8.0", false);
            }
            return new DockerHealthService.CommandResult(0, "sha256:runner", false);
        });

        RunnerHealth health = service.check();

        assertEquals("UP", health.status());
        assertTrue(health.dockerAvailable());
        assertTrue(health.runnerImageAvailable());
        assertEquals("codetest-lab-runner:latest", health.image());
    }

    @Test
    void shouldReportDegradedWhenDockerIsUpButRunnerImageIsMissing() {
        DockerHealthService service = new DockerHealthService(properties, (command, timeout) -> {
            if (command.contains("info")) {
                return new DockerHealthService.CommandResult(0, "29.8.0", false);
            }
            return new DockerHealthService.CommandResult(1, "No such image: codetest-lab-runner:latest", false);
        });

        RunnerHealth health = service.check();

        assertEquals("DEGRADED", health.status());
        assertTrue(health.dockerAvailable());
        assertFalse(health.runnerImageAvailable());
    }

    @Test
    void shouldReportDownWhenDockerDaemonIsUnavailable() {
        DockerHealthService service = new DockerHealthService(properties, (command, timeout) ->
                new DockerHealthService.CommandResult(
                        1,
                        "Docker Desktop is unable to start: context deadline exceeded",
                        false));

        RunnerHealth health = service.check();

        assertEquals("DOWN", health.status());
        assertFalse(health.dockerAvailable());
        assertFalse(health.runnerImageAvailable());
    }

    @Test
    void shouldReportDownWhenDockerProbeTimesOut() {
        DockerHealthService service = new DockerHealthService(properties, (command, timeout) ->
                new DockerHealthService.CommandResult(-1, "", true));

        RunnerHealth health = service.check();

        assertEquals("DOWN", health.status());
        assertFalse(health.dockerAvailable());
        assertTrue(health.message().toLowerCase().contains("tempo"));
    }
}
