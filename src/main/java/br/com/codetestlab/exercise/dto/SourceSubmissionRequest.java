package br.com.codetestlab.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceSubmissionRequest(
        @NotBlank @Size(max = 120) String studentName,
        @NotBlank @Size(max = 100_000) String sourceCode
) {
}
