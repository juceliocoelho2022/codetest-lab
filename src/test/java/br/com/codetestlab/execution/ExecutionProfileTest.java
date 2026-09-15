package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionProfileTest {

    @Test
    void shouldExposeCapabilitiesForAllProfiles() {
        assertFalse(ExecutionProfile.JUNIT5.mockitoEnabled());
        assertFalse(ExecutionProfile.JUNIT5.jacocoEnabled());

        assertTrue(ExecutionProfile.JUNIT5_MOCKITO.mockitoEnabled());
        assertFalse(ExecutionProfile.JUNIT5_MOCKITO.jacocoEnabled());

        assertFalse(ExecutionProfile.JUNIT5_JACOCO.mockitoEnabled());
        assertTrue(ExecutionProfile.JUNIT5_JACOCO.jacocoEnabled());

        assertTrue(ExecutionProfile.JUNIT5_MOCKITO_JACOCO.mockitoEnabled());
        assertTrue(ExecutionProfile.JUNIT5_MOCKITO_JACOCO.jacocoEnabled());
    }

    @Test
    void shouldKeepBaseTimeoutForProfilesWithoutJacoco() {
        assertEquals(20, ExecutionProfile.JUNIT5.timeoutSeconds(20));
        assertEquals(20, ExecutionProfile.JUNIT5_MOCKITO.timeoutSeconds(20));
    }

    @Test
    void shouldAllowFortySecondsForProfilesWithJacoco() {
        assertEquals(40, ExecutionProfile.JUNIT5_JACOCO.timeoutSeconds(20));
        assertEquals(40, ExecutionProfile.JUNIT5_MOCKITO_JACOCO.timeoutSeconds(20));
    }

    @Test
    void shouldRespectHigherConfiguredTimeoutForJacocoProfiles() {
        assertEquals(60, ExecutionProfile.JUNIT5_JACOCO.timeoutSeconds(60));
        assertEquals(60, ExecutionProfile.JUNIT5_MOCKITO_JACOCO.timeoutSeconds(60));
    }

    @Test
    void shouldUseMockitoProfileAsBackwardCompatibleDefault() {
        assertSame(ExecutionProfile.JUNIT5_MOCKITO, ExecutionProfile.fromNullable(null));
        assertSame(ExecutionProfile.JUNIT5_MOCKITO, ExecutionProfile.fromNullable("  "));
    }

    @Test
    void shouldParseKnownProfileIgnoringCase() {
        assertSame(ExecutionProfile.JUNIT5_MOCKITO_JACOCO,
                ExecutionProfile.fromNullable("junit5_mockito_jacoco"));
    }

    @Test
    void shouldRejectUnknownProfile() {
        assertThrows(IllegalArgumentException.class,
                () -> ExecutionProfile.fromNullable("JUNIT4"));
    }

    @Test
    void executionRequestShouldDefaultProfileWhenOldFactoryIsUsed() {
        ExecutionRequest request = ExecutionRequest.source(
                "Calculadora",
                "public class Calculadora {}",
                "class CalculadoraTest {}");

        assertSame(ExecutionProfile.JUNIT5_MOCKITO, request.executionProfile());
    }
}
