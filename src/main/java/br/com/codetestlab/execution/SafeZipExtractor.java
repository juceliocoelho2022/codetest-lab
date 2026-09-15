package br.com.codetestlab.execution;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SafeZipExtractor {
    private static final String JAVA_ROOT = "src/main/java/";
    private final int maxEntries;
    private final long maxExtractedBytes;

    public SafeZipExtractor(int maxEntries, long maxExtractedBytes) {
        if (maxEntries <= 0 || maxExtractedBytes <= 0) {
            throw new IllegalArgumentException("Limites do ZIP devem ser positivos.");
        }
        this.maxEntries = maxEntries;
        this.maxExtractedBytes = maxExtractedBytes;
    }

    public int extractJavaSources(byte[] zipBytes, Path destination) throws IOException {
        if (zipBytes == null || zipBytes.length == 0) throw new IllegalArgumentException("ZIP vazio.");
        Files.createDirectories(destination);
        Path root = destination.toAbsolutePath().normalize();
        int entries = 0;
        int javaFiles = 0;
        long totalBytes = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > maxEntries) throw new IllegalArgumentException("ZIP possui entradas demais.");

                String name = entry.getName().replace('\\', '/');
                rejectTraversal(name);

                if (entry.isDirectory() || !name.startsWith(JAVA_ROOT) || !name.endsWith(".java")) {
                    zip.closeEntry();
                    continue;
                }

                String relative = name.substring(JAVA_ROOT.length());
                if (relative.isBlank()) {
                    zip.closeEntry();
                    continue;
                }

                Path target = root.resolve(relative).normalize();
                if (!target.startsWith(root)) throw new IllegalArgumentException("Caminho inválido no ZIP.");
                Files.createDirectories(target.getParent());

                try (OutputStream out = Files.newOutputStream(target)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        totalBytes += read;
                        if (totalBytes > maxExtractedBytes) {
                            throw new IllegalArgumentException("Conteúdo descompactado excede o limite permitido.");
                        }
                        out.write(buffer, 0, read);
                    }
                }
                javaFiles++;
                zip.closeEntry();
            }
        }

        if (javaFiles == 0) {
            throw new IllegalArgumentException("O ZIP deve conter arquivos .java dentro de src/main/java.");
        }
        return javaFiles;
    }

    private void rejectTraversal(String name) {
        if (name.startsWith("/") || name.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("Caminho absoluto não permitido no ZIP.");
        }
        Path normalized = Path.of(name).normalize();
        if (normalized.startsWith("..") || name.contains("../")) {
            throw new IllegalArgumentException("Path traversal detectado no ZIP.");
        }
    }
}
