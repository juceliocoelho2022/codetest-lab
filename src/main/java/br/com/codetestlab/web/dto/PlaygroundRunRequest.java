package br.com.codetestlab.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaygroundRunRequest(
        @NotBlank @Size(max = 180) String className,
        @NotBlank @Size(max = 100_000) String sourceCode,
        @NotBlank @Size(max = 100_000) String testCode
) {
}
