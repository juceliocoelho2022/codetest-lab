package br.com.codetestlab.execution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExecutionOutputParser {
    private static final Pattern TEST_SUMMARY = Pattern.compile(
            "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)",
            Pattern.CASE_INSENSITIVE);

    public ExecutionResult parse(int exitCode, String output, long durationMs) {
        String safeOutput = output == null ? "" : output;
        int testsRun = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;

        Matcher matcher = TEST_SUMMARY.matcher(safeOutput);
        while (matcher.find()) {
            testsRun = Integer.parseInt(matcher.group(1));
            failures = Integer.parseInt(matcher.group(2));
            errors = Integer.parseInt(matcher.group(3));
            skipped = Integer.parseInt(matcher.group(4));
        }

        int failed = failures + errors;
        int passed = Math.max(0, testsRun - failed - skipped);
        ExecutionStatus status;
        if (exitCode == 0) {
            status = ExecutionStatus.PASSED;
        } else if (containsCompileFailure(safeOutput)) {
            status = ExecutionStatus.COMPILE_ERROR;
        } else {
            status = ExecutionStatus.FAILED;
        }

        return new ExecutionResult(status, testsRun, passed, failed, skipped, durationMs, safeOutput);
    }

    private boolean containsCompileFailure(String output) {
        String normalized = output.toLowerCase();
        return normalized.contains("compilation error")
                || normalized.contains("compilation failure")
                || normalized.contains("cannot find symbol")
                || normalized.contains("failed to execute goal org.apache.maven.plugins:maven-compiler-plugin");
    }
}
