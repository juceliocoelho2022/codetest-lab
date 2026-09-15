package br.com.codetestlab.execution;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum ExecutionProfile {
    JUNIT5(false, false),
    JUNIT5_MOCKITO(true, false),
    JUNIT5_JACOCO(false, true),
    JUNIT5_MOCKITO_JACOCO(true, true);

    private static final int JACOCO_MIN_TIMEOUT_SECONDS = 40;

    private final boolean mockitoEnabled;
    private final boolean jacocoEnabled;

    ExecutionProfile(boolean mockitoEnabled, boolean jacocoEnabled) {
        this.mockitoEnabled = mockitoEnabled;
        this.jacocoEnabled = jacocoEnabled;
    }

    public boolean mockitoEnabled() {
        return mockitoEnabled;
    }

    public boolean jacocoEnabled() {
        return jacocoEnabled;
    }

    public int timeoutSeconds(int configuredTimeoutSeconds) {
        return jacocoEnabled
                ? Math.max(configuredTimeoutSeconds, JACOCO_MIN_TIMEOUT_SECONDS)
                : configuredTimeoutSeconds;
    }

    public static ExecutionProfile defaultProfile() {
        return JUNIT5_MOCKITO;
    }

    public static ExecutionProfile fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return defaultProfile();
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String accepted = Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Perfil de execução inválido: " + value + ". Valores aceitos: " + accepted + ".");
        }
    }
}
