package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeZipExtractorTest {
    @TempDir Path tempDir;

    @Test
    void shouldExtractOnlyMainJavaSources() throws Exception {
        byte[] zip = zipOf("src/main/java/demo/App.java", "package demo; public class App {}", "pom.xml", "ignored");
        int count = new SafeZipExtractor(20, 100_000).extractJavaSources(zip, tempDir);
        assertEquals(1, count);
        assertTrue(Files.exists(tempDir.resolve("demo/App.java")));
    }

    @Test
    void shouldRejectPathTraversal() throws Exception {
        byte[] zip = zipOf("../evil.java", "class Evil {}", null, null);
        assertThrows(IllegalArgumentException.class,
                () -> new SafeZipExtractor(20, 100_000).extractJavaSources(zip, tempDir));
    }

    private byte[] zipOf(String firstName, String firstContent, String secondName, String secondContent) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(firstName));
            zip.write(firstContent.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if (secondName != null) {
                zip.putNextEntry(new ZipEntry(secondName));
                zip.write(secondContent.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
