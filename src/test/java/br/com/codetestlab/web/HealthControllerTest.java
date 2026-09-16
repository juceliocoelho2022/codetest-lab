package br.com.codetestlab.web;

import br.com.codetestlab.execution.DockerHealthService;
import br.com.codetestlab.execution.RunnerHealth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void shouldKeepApplicationHealthEndpoint() {
        DockerHealthService service = mock(DockerHealthService.class);
        HealthController controller = new HealthController(service);

        assertEquals("UP", controller.health().get("status"));
        assertEquals("CodeTest Lab", controller.health().get("application"));
    }

    @Test
    void shouldExposeRunnerHealth() {
        DockerHealthService service = mock(DockerHealthService.class);
        RunnerHealth expected = new RunnerHealth(
                "UP",
                true,
                true,
                "codetest-lab-runner:latest",
                "Docker e runner prontos para executar testes.");
        when(service.check()).thenReturn(expected);

        HealthController controller = new HealthController(service);

        assertSame(expected, controller.runnerHealth());
    }
}
