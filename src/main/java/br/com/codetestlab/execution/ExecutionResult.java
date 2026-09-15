package br.com.codetestlab.execution;

public record ExecutionResult(
        ExecutionStatus status,
        int testsRun,
        int testsPassed,
        int testsFailed,
        int testsSkipped,
        long durationMs,
        String output
) {
    public static ExecutionResult timeout(long durationMs, String output) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, 0, 0, 0, 0, durationMs, output);
    }

    public static ExecutionResult infrastructureError(long durationMs, String output) {
        return new ExecutionResult(ExecutionStatus.INFRASTRUCTURE_ERROR, 0, 0, 0, 0, durationMs, output);
    }
}
