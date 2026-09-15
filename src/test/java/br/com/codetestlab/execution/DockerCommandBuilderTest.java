package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerCommandBuilderTest {
    @Test
    void shouldBuildHardenedDockerCommand() {
        var command = new DockerCommandBuilder().build(Path.of("/tmp/code"), "codetest-lab-runner:latest");
        assertTrue(command.contains("--network"));
        assertTrue(command.contains("none"));
        assertTrue(command.contains("--read-only"));
        assertTrue(command.contains("--cap-drop"));
        assertTrue(command.contains("ALL"));
        assertTrue(command.contains("no-new-privileges"));
        assertTrue(command.contains("/tmp:rw,noexec,nosuid,size=64m"));
        assertTrue(command.contains("/jansi-tmp:rw,exec,nosuid,nodev,size=8m"));
        assertTrue(command.contains("MAVEN_OPTS=-Djansi.tmpdir=/jansi-tmp -Djansi.mode=strip"));
        assertTrue(command.stream().noneMatch(v -> v.equals("sh") || v.equals("bash")));
    }
}
