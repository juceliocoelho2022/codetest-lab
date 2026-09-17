package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaSourceFileTest {

    @Test
    void shouldAcceptSimpleJavaFilename() {
        JavaSourceFile source = new JavaSourceFile(
                "PedidoService.java",
                "public class PedidoService {}");

        assertEquals("PedidoService.java", source.fileName());
    }

    @Test
    void shouldRejectNonJavaExtension() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("PedidoService.txt", "class PedidoService {}"));
    }

    @Test
    void shouldRejectPathSeparatorsAndTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("../PedidoService.java", "class PedidoService {}"));
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("domain/PedidoService.java", "class PedidoService {}"));
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("domain\\PedidoService.java", "class PedidoService {}"));
    }

    @Test
    void shouldRejectInvalidJavaIdentifierFilename() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("pedido-service.java", "class PedidoService {}"));
    }

    @Test
    void shouldRejectBlankContent() {
        assertThrows(IllegalArgumentException.class, () ->
                new JavaSourceFile("PedidoService.java", "   "));
    }
}
