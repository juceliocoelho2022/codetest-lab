package br.com.codetestlab.exercise.dto;

import br.com.codetestlab.exercise.SubmissionType;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.ExecutionStatus;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long exerciseId,
        String studentName,
        SubmissionType submissionType,
        ExecutionStatus status,
        int testsRun,
        int testsPassed,
        int testsFailed,
        int testsSkipped,
        long durationMs,
        String output,
        Instant createdAt
) {
    public static SubmissionResponse transientResult(Long exerciseId, String studentName, SubmissionType type,
                                                     ExecutionResult result) {
        return new SubmissionResponse(null, exerciseId, studentName, type, result.status(), result.testsRun(),
                result.testsPassed(), result.testsFailed(), result.testsSkipped(), result.durationMs(),
                result.output(), Instant.now());
    }
}
