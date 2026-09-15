package br.com.codetestlab.exercise.dto;

import java.time.Instant;

public record ExerciseResponse(
        Long id,
        String title,
        String description,
        String className,
        Instant createdAt
) {
}
