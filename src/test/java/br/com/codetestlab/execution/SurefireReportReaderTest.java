package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurefireReportReaderTest {
    @TempDir Path tempDir;

    @Test
    void shouldReadSurefireCounters() throws Exception {
        Files.writeString(tempDir.resolve("TEST-demo.xml"),
                "<testsuite tests=\"4\" failures=\"1\" errors=\"1\" skipped=\"1\"></testsuite>");
        var fallback = new ExecutionResult(ExecutionStatus.FAILED, 0, 0, 0, 0, 42, "output");

        var result = new SurefireReportReader().enrich(tempDir, fallback);

        assertEquals(4, result.testsRun());
        assertEquals(1, result.testsPassed());
        assertEquals(2, result.testsFailed());
        assertEquals(1, result.testsSkipped());
    }
}
