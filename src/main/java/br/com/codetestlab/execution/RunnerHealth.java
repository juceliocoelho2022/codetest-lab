package br.com.codetestlab.execution;

public record RunnerHealth(
        String status,
        boolean dockerAvailable,
        boolean runnerImageAvailable,
        String image,
        String message
) {
}
