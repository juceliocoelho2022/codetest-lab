package br.com.codetestlab.web;

import br.com.codetestlab.execution.ExecutionProfile;
import br.com.codetestlab.execution.ExecutionRequest;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.ExecutionStatus;
import br.com.codetestlab.web.dto.JavaSourceFileRequest;
import br.com.codetestlab.web.dto.PlaygroundRunRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void shouldRejectRequestWithoutAnySourceForm() {
        PlaygroundController controller = new PlaygroundController(request ->
                new ExecutionResult(ExecutionStatus.PASSED, 0, 0, 0, 0, 1, ""));

        PlaygroundRunRequest request = new PlaygroundRunRequest(
                "Calculadora",
                null,
                null,
                "class CalculadoraTest {}",
                "JUNIT5");

        assertThrows(IllegalArgumentException.class, () -> controller.run(request));
    }
}
