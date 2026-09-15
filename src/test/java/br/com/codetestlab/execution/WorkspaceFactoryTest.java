package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceFactoryTest {

    @Test
    void junitOnlyShouldNotIncludeMockitoOrJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5);

        assertTrue(pom.contains("junit-jupiter"));
        assertFalse(pom.contains("mockito-junit-jupiter"));
        assertFalse(pom.contains("jacoco-maven-plugin"));
    }

    @Test
    void mockitoProfileShouldIncludeMockitoWithoutJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_MOCKITO);

        assertTrue(pom.contains("mockito-junit-jupiter"));
        assertFalse(pom.contains("jacoco-maven-plugin"));
    }

    @Test
    void jacocoProfileShouldIncludeJacocoWithoutMockito() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_JACOCO);

        assertFalse(pom.contains("mockito-junit-jupiter"));
        assertTrue(pom.contains("jacoco-maven-plugin"));
        assertTrue(pom.contains("<goal>prepare-agent</goal>"));
        assertTrue(pom.contains("<goal>report</goal>"));
        assertTrue(pom.contains("<phase>verify</phase>"));
    }

    @Test
    void fullProfileShouldIncludeMockitoAndJacoco() {
        String pom = WorkspaceFactory.runnerPom(ExecutionProfile.JUNIT5_MOCKITO_JACOCO);

        assertTrue(pom.contains("mockito-junit-jupiter"));
        assertTrue(pom.contains("jacoco-maven-plugin"));
    }
}
