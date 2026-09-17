package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

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
        List<JavaSourceFile> files = IntStream.rangeClosed(1, 11)
                .mapToObj(i -> new JavaSourceFile("Classe" + i + ".java", "class Classe" + i + " {}"))
                .toList();

        assertThrows(IllegalArgumentException.class, () ->
                ExecutionRequest.sourceFiles("Classe1", files, "class Classe1Test {}", ExecutionProfile.JUNIT5));
    }

    @Test
    void shouldRejectAggregateSourceOverOneHundredThousandCharacters() {
        String large = "a".repeat(50_001);
        assertThrows(IllegalArgumentException.class, () ->
                ExecutionRequest.sourceFiles(
                        "Classe1",
                        List.of(
                                new JavaSourceFile("Classe1.java", large),
                                new JavaSourceFile("Classe2.java", large)),
                        "class Classe1Test {}",
                        ExecutionProfile.JUNIT5));
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
