package br.com.codetestlab.execution;

import java.util.Arrays;
import java.util.Objects;

public record ExecutionRequest(
        String className,
        String sourceCode,
        byte[] zipBytes,
        String testCode
) {
    public ExecutionRequest {
        className = requireText(className, "className");
        testCode = requireText(testCode, "testCode");
        boolean hasSource = sourceCode != null && !sourceCode.isBlank();
        boolean hasZip = zipBytes != null && zipBytes.length > 0;
        if (hasSource == hasZip) {
            throw new IllegalArgumentException("Informe exatamente uma origem: sourceCode ou zipBytes.");
        }
        zipBytes = zipBytes == null ? null : Arrays.copyOf(zipBytes, zipBytes.length);
    }

    public static ExecutionRequest source(String className, String sourceCode, String testCode) {
        return new ExecutionRequest(className, requireText(sourceCode, "sourceCode"), null, testCode);
    }

    public static ExecutionRequest zip(String className, byte[] zipBytes, String testCode) {
        Objects.requireNonNull(zipBytes, "zipBytes");
        return new ExecutionRequest(className, null, zipBytes, testCode);
    }

    public boolean isZip() {
        return zipBytes != null;
    }

    @Override
    public byte[] zipBytes() {
        return zipBytes == null ? null : Arrays.copyOf(zipBytes, zipBytes.length);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
        return value;
    }
}
