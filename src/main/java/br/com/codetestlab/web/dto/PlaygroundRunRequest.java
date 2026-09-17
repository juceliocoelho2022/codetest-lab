package br.com.codetestlab.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaygroundRunRequest(
        @NotBlank @Size(max = 180) String className,
        @Size(max = 100_000) String sourceCode,
        @Size(max = 10) List<@Valid JavaSourceFileRequest> sourceFiles,
        @NotBlank @Size(max = 100_000) String testCode,
        @Size(max = 80) String executionProfile
) {
    public PlaygroundRunRequest(String className, String sourceCode, String testCode) {
        this(className, sourceCode, null, testCode, null);
    }

    public PlaygroundRunRequest(String className,
                                String sourceCode,
                                String testCode,
                                String executionProfile) {
        this(className, sourceCode, null, testCode, executionProfile);
    }
}
