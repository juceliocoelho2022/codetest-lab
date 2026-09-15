package br.com.codetestlab.web;

import br.com.codetestlab.execution.ExecutionProfile;
import br.com.codetestlab.execution.ExecutionRequest;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.ExecutionStatus;
import br.com.codetestlab.web.dto.PlaygroundRunRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaygroundControllerTest {

    @Test
    void shouldUseDefaultProfileWhenRequestOmitsIt() {
        AtomicReference<ExecutionRequest> captured = new AtomicReference<>();
        PlaygroundController controller = new PlaygroundController(request -> {
            captured.set(request);
            return new ExecutionResult(ExecutionStatus.PASSED, 1, 1, 0, 0, 1, "");
        });

        controller.run(new PlaygroundRunRequest(
                "Calculadora",
                "public class Calculadora {}",
                "class CalculadoraTest {}"));

        assertSame(ExecutionProfile.JUNIT5_MOCKITO, captured.get().executionProfile());
    }

    @Test
    void shouldForwardSelectedProfileToExecutor() {
        AtomicReference<ExecutionRequest> captured = new AtomicReference<>();
        PlaygroundController controller = new PlaygroundController(request -> {
            captured.set(request);
            return new ExecutionResult(ExecutionStatus.PASSED, 1, 1, 0, 0, 1, "");
        });

        controller.run(new PlaygroundRunRequest(
                "Calculadora",
                "public class Calculadora {}",
                "class CalculadoraTest {}",
                "JUNIT5_MOCKITO_JACOCO"));

        assertEquals(ExecutionProfile.JUNIT5_MOCKITO_JACOCO, captured.get().executionProfile());
    }

    @Test
    void shouldRejectUnknownProfileBeforeExecution() {
        PlaygroundController controller = new PlaygroundController(request ->
                new ExecutionResult(ExecutionStatus.PASSED, 0, 0, 0, 0, 1, ""));

        PlaygroundRunRequest request = new PlaygroundRunRequest(
                "Calculadora",
                "public class Calculadora {}",
                "class CalculadoraTest {}",
                "JUNIT4");

        assertThrows(IllegalArgumentException.class, () -> controller.run(request));
    }
}
