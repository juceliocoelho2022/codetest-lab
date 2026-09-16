package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionOutputParserTest {
    private final ExecutionOutputParser parser = new ExecutionOutputParser();

    @Test
    void shouldParsePassingTests() {
        var result = parser.parse(0, "Tests run: 4, Failures: 0, Errors: 0, Skipped: 1", 25);
        assertEquals(ExecutionStatus.PASSED, result.status());
        assertEquals(4, result.testsRun());
        assertEquals(3, result.testsPassed());
        assertEquals(1, result.testsSkipped());
    }

    @Test
    void shouldParseFailedTests() {
        var result = parser.parse(1, "Tests run: 3, Failures: 1, Errors: 1, Skipped: 0", 30);
        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals(2, result.testsFailed());
    }

    @Test
    void shouldDetectCompilationError() {
        var result = parser.parse(1, "[ERROR] COMPILATION ERROR cannot find symbol", 10);
        assertEquals(ExecutionStatus.COMPILE_ERROR, result.status());
    }

    @Test
    void shouldDetectDockerDaemonUnavailableAsInfrastructureError() {
        String output = "Error response from daemon: Docker Desktop is unable to start\n"
                + "docker daemon did not become ready: context deadline exceeded";

        var result = parser.parse(1, output, 1_200);

        assertEquals(ExecutionStatus.INFRASTRUCTURE_ERROR, result.status());
    }

    @Test
    void shouldDetectMissingRunnerImageAsInfrastructureError() {
        String output = "docker: Error response from daemon: pull access denied for codetest-lab-runner, "
                + "repository does not exist or may require 'docker login'";

        var result = parser.parse(125, output, 650);

        assertEquals(ExecutionStatus.INFRASTRUCTURE_ERROR, result.status());
    }

    @Test
    void shouldKeepTestFailureWhenDockerWordsAreOnlyPartOfTestOutput() {
        String output = "Tests run: 1, Failures: 1, Errors: 0, Skipped: 0\n"
                + "Assertion failed: Error response from daemon was expected in the message";

        var result = parser.parse(1, output, 40);

        assertEquals(ExecutionStatus.FAILED, result.status());
        assertEquals(1, result.testsRun());
    }
}
