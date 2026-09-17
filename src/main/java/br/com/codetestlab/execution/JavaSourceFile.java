package br.com.codetestlab.execution;

import java.util.regex.Pattern;

public record JavaSourceFile(String fileName, String content) {
    private static final Pattern JAVA_IDENTIFIER =
            Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    public JavaSourceFile {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName é obrigatório.");
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Nome de arquivo Java inválido.");
        }
        if (!fileName.endsWith(".java")) {
            throw new IllegalArgumentException("O arquivo deve terminar em .java.");
        }

        String baseName = fileName.substring(0, fileName.length() - ".java".length());
        if (!JAVA_IDENTIFIER.matcher(baseName).matches()) {
            throw new IllegalArgumentException("Nome de arquivo Java inválido.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content é obrigatório.");
        }
    }
}
