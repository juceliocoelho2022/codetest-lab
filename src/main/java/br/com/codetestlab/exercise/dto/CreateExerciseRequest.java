package br.com.codetestlab.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 5_000) String description,
        @NotBlank @Size(max = 180) String className,
        @NotBlank @Size(max = 100_000) String hiddenTestCode
) {
}
