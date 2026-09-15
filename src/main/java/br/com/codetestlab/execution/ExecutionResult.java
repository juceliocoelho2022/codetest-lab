package br.com.codetestlab.execution;

public record ExecutionResult(
        ExecutionStatus status,
        int testsRun,
        int testsPassed,
        int testsFailed,
        int testsSkipped,
        long durationMs,
        String output,
        CoverageResult coverage
) {
    public ExecutionResult(ExecutionStatus status,
                           int testsRun,
                           int testsPassed,
                           int testsFailed,
                           int testsSkipped,
                           long durationMs,
                           String output) {
        this(status, testsRun, testsPassed, testsFailed, testsSkipped, durationMs, output, null);
    }

    public static ExecutionResult timeout(long durationMs, String output) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, 0, 0, 0, 0, durationMs, output, null);
    }

    public static ExecutionResult infrastructureError(long durationMs, String output) {
        return new ExecutionResult(ExecutionStatus.INFRASTRUCTURE_ERROR, 0, 0, 0, 0, durationMs, output, null);
    }
}
