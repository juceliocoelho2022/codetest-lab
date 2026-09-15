package br.com.codetestlab.execution;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DockerCommandBuilder {
    public List<String> build(Path workspace, String image) {
        if (workspace == null) throw new IllegalArgumentException("workspace é obrigatório.");
        if (image == null || image.isBlank()) throw new IllegalArgumentException("image é obrigatória.");

        String mount = workspace.toAbsolutePath().normalize() + ":/workspace:rw";
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--network");
        command.add("none");
        command.add("--read-only");
        command.add("--memory");
        command.add("384m");
        command.add("--cpus");
        command.add("1.0");
        command.add("--pids-limit");
        command.add("128");
        command.add("--cap-drop");
        command.add("ALL");
        command.add("--security-opt");
        command.add("no-new-privileges");
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=64m");
        command.add("--tmpfs");
        command.add("/jansi-tmp:rw,exec,nosuid,nodev,size=8m");
        command.add("--env");
        command.add("MAVEN_OPTS=-Djansi.tmpdir=/jansi-tmp -Djansi.mode=strip");
        command.add("-v");
        command.add(mount);
        command.add("-w");
        command.add("/workspace");
        command.add(image);
        return List.copyOf(command);
    }
}
