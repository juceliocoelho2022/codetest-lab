package br.com.codetestlab.execution;

import br.com.codetestlab.config.ExecutionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class DockerHealthService {
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(4);
    private static final int MAX_MESSAGE_CHARS = 240;

    private final ExecutionProperties properties;
    private final CommandRunner commandRunner;

    @Autowired
    public DockerHealthService(ExecutionProperties properties) {
        this(properties, new ProcessCommandRunner());
    }

    DockerHealthService(ExecutionProperties properties, CommandRunner commandRunner) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");
    }

    public RunnerHealth check() {
        try {
            CommandResult docker = commandRunner.run(
                    List.of("docker", "info", "--format", "{{.ServerVersion}}"),
                    PROBE_TIMEOUT);

            if (docker.timedOut()) {
                return down("O Docker não respondeu dentro do tempo limite da verificação.");
            }
            if (!docker.success()) {
                return down(messageOrDefault(
                        docker.output(),
                        "Docker Desktop/daemon indisponível."));
            }

            CommandResult runner = commandRunner.run(
                    List.of("docker", "image", "inspect", properties.image(), "--format", "{{.Id}}"),
                    PROBE_TIMEOUT);

            if (runner.timedOut()) {
                return new RunnerHealth(
                        "DEGRADED",
                        true,
                        false,
                        properties.image(),
                        "Docker está online, mas a verificação da imagem runner excedeu o tempo limite.");
            }
            if (!runner.success()) {
                return new RunnerHealth(
                        "DEGRADED",
                        true,
                        false,
                        properties.image(),
                        messageOrDefault(
                                runner.output(),
                                "Docker está online, mas a imagem runner não está disponível."));
            }

            return new RunnerHealth(
                    "UP",
                    true,
                    true,
                    properties.image(),
                    "Docker e runner prontos para executar testes.");
        } catch (IOException e) {
            return down("Não foi possível executar o Docker CLI: " + safeMessage(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return down("A verificação do Docker foi interrompida.");
        }
    }

    private RunnerHealth down(String message) {
        return new RunnerHealth(
                "DOWN",
                false,
                false,
                properties.image(),
                message);
    }

    private String messageOrDefault(String output, String fallback) {
        String normalized = safeMessage(output);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String safeMessage(String value) {
        if (value == null) return "";
        String normalized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= MAX_MESSAGE_CHARS) return normalized;
        return normalized.substring(0, MAX_MESSAGE_CHARS) + "…";
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String output, boolean timedOut) {
        boolean success() {
            return !timedOut && exitCode == 0;
        }
    }

    private static final class ProcessCommandRunner implements CommandRunner {
        @Override
        public CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            Thread reader = Thread.ofVirtual().start(() -> drain(process, output));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }

            reader.join(1_000);
            return new CommandResult(
                    finished ? process.exitValue() : -1,
                    output.toString(),
                    !finished);
        }

        private static void drain(Process process, StringBuilder output) {
            try (var reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    output.append(buffer, 0, read);
                    if (output.length() > 4_096) {
                        output.setLength(4_096);
                        break;
                    }
                }
            } catch (IOException ignored) {
                // A forced timeout can close the process stream while it is being drained.
            }
        }
    }
}
