package br.com.codetestlab.execution;

import br.com.codetestlab.config.ExecutionProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceFactoryTest {

    @Test
    void junitOnlyShouldNotIncludeMockitoOrJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5);

        assertTrue(pom.contains("junit-jupiter"));
        assertFalse(pom.contains("mockito-junit-jupiter"));
        assertFalse(pom.contains("jacoco-maven-plugin"));
    }

    @Test
    void mockitoProfileShouldIncludeMockitoWithoutJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_MOCKITO);

        assertTrue(pom.contains("mockito-junit-jupiter"));
        assertFalse(pom.contains("jacoco-maven-plugin"));
    }

    @Test
    void jacocoProfileShouldIncludeJacocoWithoutMockito() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_JACOCO);

        assertFalse(pom.contains("mockito-junit-jupiter"));
        assertTrue(pom.contains("jacoco-maven-plugin"));
        assertTrue(pom.contains("<goal>prepare-agent</goal>"));
        assertTrue(pom.contains("<goal>report</goal>"));
        assertTrue(pom.contains("<phase>verify</phase>"));
    }

    @Test
    void fullProfileShouldIncludeMockitoAndJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_MOCKITO_JACOCO);

        assertTrue(pom.contains("mockito-junit-jupiter"));
        assertTrue(pom.contains("jacoco-maven-plugin"));
    }

    @Test
    void shouldMaterializeMultipleJavaSourceFiles() throws Exception {
        WorkspaceFactory factory = new WorkspaceFactory(properties(100_000));
        ExecutionRequest request = ExecutionRequest.sourceFiles(
                "PedidoService",
                List.of(
                        new JavaSourceFile(
                                "PedidoService.java",
                                "public class PedidoService { private final EstoqueRepository repo = null; }"),
                        new JavaSourceFile(
                                "EstoqueRepository.java",
                                "public interface EstoqueRepository {}")),
                "class PedidoServiceTest {}",
                ExecutionProfile.JUNIT5_MOCKITO);

        Path workspace = factory.create(request);
        try {
            Path mainRoot = workspace.resolve("src/main/java");
            assertEquals(
                    "public class PedidoService { private final EstoqueRepository repo = null; }",
                    Files.readString(mainRoot.resolve("PedidoService.java")));
            assertEquals(
                    "public interface EstoqueRepository {}",
                    Files.readString(mainRoot.resolve("EstoqueRepository.java")));
        } finally {
            deleteRecursively(workspace);
        }
    }

    @Test
    void shouldEnforceAggregateSourceLimitAtWorkspaceBoundary() {
        WorkspaceFactory factory = new WorkspaceFactory(properties(30));
        ExecutionRequest request = ExecutionRequest.sourceFiles(
                "ClasseA",
                List.of(
                        new JavaSourceFile("ClasseA.java", "public class ClasseA {}"),
                        new JavaSourceFile("ClasseB.java", "public class ClasseB {}")),
                "class ClasseATest {}",
                ExecutionProfile.JUNIT5);

        assertThrows(IllegalArgumentException.class, () -> factory.create(request));
    }

    private ExecutionProperties properties(int maxSourceChars) {
        return new ExecutionProperties(
                "codetest-lab-runner:latest",
                20,
                20_000,
                maxSourceChars,
                1_048_576,
                200,
                2_000_000);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
