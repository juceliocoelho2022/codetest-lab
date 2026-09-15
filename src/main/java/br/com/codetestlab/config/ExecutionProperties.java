package br.com.codetestlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codetest.execution")
public record ExecutionProperties(
        String image,
        int timeoutSeconds,
        int maxOutputChars,
        int maxSourceChars,
        int maxZipBytes,
        int maxZipEntries,
        long maxExtractedBytes
) {
    public ExecutionProperties {
        if (image == null || image.isBlank()) image = "codetest-lab-runner:latest";
        if (timeoutSeconds <= 0) timeoutSeconds = 20;
        if (maxOutputChars <= 0) maxOutputChars = 20_000;
        if (maxSourceChars <= 0) maxSourceChars = 100_000;
        if (maxZipBytes <= 0) maxZipBytes = 1_048_576;
        if (maxZipEntries <= 0) maxZipEntries = 200;
        if (maxExtractedBytes <= 0) maxExtractedBytes = 2_000_000;
    }
}
