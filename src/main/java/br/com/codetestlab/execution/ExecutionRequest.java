package br.com.codetestlab.execution;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record ExecutionRequest(
        String className,
        List<JavaSourceFile> sourceFiles,
        byte[] zipBytes,
        String testCode,
        ExecutionProfile executionProfile
) {
    private static final int MAX_SOURCE_FILES = 10;
    private static final int MAX_SOURCE_CHARS = 100_000;

    public ExecutionRequest {
        className = requireText(className, "className");
        testCode = requireText(testCode, "testCode");
        executionProfile = executionProfile == null ? ExecutionProfile.defaultProfile() : executionProfile;

        List<JavaSourceFile> files = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
        boolean hasSourceFiles = !files.isEmpty();
        boolean hasZip = zipBytes != null && zipBytes.length > 0;
        if (hasSourceFiles == hasZip) {
            throw new IllegalArgumentException("Informe exatamente uma origem: sourceFiles ou zipBytes.");
        }

        if (hasSourceFiles) {
            validateSourceFiles(className, files);
        }

        sourceFiles = files;
        zipBytes = zipBytes == null ? null : Arrays.copyOf(zipBytes, zipBytes.length);
    }

    public ExecutionRequest(String className,
                            String sourceCode,
                            byte[] zipBytes,
                            String testCode,
                            ExecutionProfile executionProfile) {
        this(
                className,
                sourceCode == null || sourceCode.isBlank()
                        ? List.of()
                        : List.of(new JavaSourceFile(
                                WorkspaceFactory.simpleClassName(className) + ".java",
                                sourceCode)),
                zipBytes,
                testCode,
                executionProfile);
    }

    public ExecutionRequest(String className, String sourceCode, byte[] zipBytes, String testCode) {
        this(className, sourceCode, zipBytes, testCode, ExecutionProfile.defaultProfile());
    }

    public static ExecutionRequest source(String className, String sourceCode, String testCode) {
        return source(className, sourceCode, testCode, ExecutionProfile.defaultProfile());
    }

    public static ExecutionRequest source(String className,
                                          String sourceCode,
                                          String testCode,
                                          ExecutionProfile executionProfile) {
        String fileName = WorkspaceFactory.simpleClassName(className) + ".java";
        return sourceFiles(
                className,
                List.of(new JavaSourceFile(fileName, requireText(sourceCode, "sourceCode"))),
                testCode,
                executionProfile);
    }

    public static ExecutionRequest sourceFiles(String className,
                                               List<JavaSourceFile> sourceFiles,
                                               String testCode,
                                               ExecutionProfile executionProfile) {
        Objects.requireNonNull(sourceFiles, "sourceFiles");
        return new ExecutionRequest(className, sourceFiles, null, testCode, executionProfile);
    }

    public static ExecutionRequest zip(String className, byte[] zipBytes, String testCode) {
        return zip(className, zipBytes, testCode, ExecutionProfile.defaultProfile());
    }

    public static ExecutionRequest zip(String className,
                                       byte[] zipBytes,
                                       String testCode,
                                       ExecutionProfile executionProfile) {
        Objects.requireNonNull(zipBytes, "zipBytes");
        return new ExecutionRequest(className, List.of(), zipBytes, testCode, executionProfile);
    }

    public boolean isZip() {
        return zipBytes != null;
    }

    /**
     * Compatibility accessor used by the legacy single-file workspace path.
     * Multi-file workspace creation uses {@link #sourceFiles()} directly.
     */
    public String sourceCode() {
        if (isZip() || sourceFiles.isEmpty()) return null;
        String primaryFile = WorkspaceFactory.simpleClassName(className) + ".java";
        return sourceFiles.stream()
                .filter(file -> file.fileName().equals(primaryFile))
                .findFirst()
                .map(JavaSourceFile::content)
                .orElse(null);
    }

    @Override
    public List<JavaSourceFile> sourceFiles() {
        return List.copyOf(sourceFiles);
    }

    @Override
    public byte[] zipBytes() {
        return zipBytes == null ? null : Arrays.copyOf(zipBytes, zipBytes.length);
    }

    private static void validateSourceFiles(String className, List<JavaSourceFile> files) {
        if (files.size() > MAX_SOURCE_FILES) {
            throw new IllegalArgumentException("O limite é de 10 arquivos Java.");
        }

        long totalChars = files.stream()
                .mapToLong(file -> file.content().length())
                .sum();
        if (totalChars > MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException(
                    "O código-fonte excede o limite total de 100000 caracteres.");
        }

        long uniqueNames = files.stream()
                .map(file -> file.fileName().toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (uniqueNames != files.size()) {
            throw new IllegalArgumentException("Nomes de arquivo Java duplicados não são permitidos.");
        }

        String primaryFile = WorkspaceFactory.simpleClassName(className) + ".java";
        boolean primaryPresent = files.stream()
                .anyMatch(file -> file.fileName().equals(primaryFile));
        if (!primaryPresent) {
            throw new IllegalArgumentException("O arquivo principal " + primaryFile + " é obrigatório.");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
        return value;
    }
}
