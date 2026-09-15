package br.com.codetestlab.execution;

import br.com.codetestlab.config.ExecutionProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerCodeExecutor implements CodeExecutor {
    private final ExecutionProperties properties;
    private final DockerCommandBuilder commandBuilder = new DockerCommandBuilder();
    private final ExecutionOutputParser outputParser = new ExecutionOutputParser();
    private final SurefireReportReader reportReader = new SurefireReportReader();
    private final JacocoReportReader jacocoReportReader = new JacocoReportReader();

    public DockerCodeExecutor(ExecutionProperties properties) {
        this.properties = properties;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        long start = System.nanoTime();
        Path workspace = null;
        try {
            workspace = new WorkspaceFactory(properties).create(request);
            List<String> command = commandBuilder.build(workspace, properties.image());
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            Thread reader = Thread.ofVirtual().start(() -> drain(process, output));
            boolean finished = process.waitFor(properties.timeoutSeconds(), TimeUnit.SECONDS);
            long durationMs = elapsedMs(start);

            if (!finished) {
                process.destroyForcibly();
                reader.join(2_000);
                return ExecutionResult.timeout(durationMs, limited(output.toString()));
            }

            reader.join(2_000);
            ExecutionResult parsed = outputParser.parse(process.exitValue(), limited(output.toString()), durationMs);
            ExecutionResult enriched = reportReader.enrich(workspace.resolve("target/surefire-reports"), parsed);

            if (request.executionProfile().jacocoEnabled()
                    && enriched.status() != ExecutionStatus.COMPILE_ERROR) {
                enriched = jacocoReportReader.enrich(
                        workspace.resolve("target/site/jacoco/jacoco.xml"),
                        enriched);
            }

            return enriched;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            return ExecutionResult.infrastructureError(elapsedMs(start),
                    "Não foi possível iniciar o Docker. Confirme se o Docker Desktop está ativo e se a imagem '"
                            + properties.image() + "' foi criada. Detalhe: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.infrastructureError(elapsedMs(start), "Execução interrompida.");
        } finally {
            deleteRecursively(workspace);
        }
    }

    private void drain(Process process, StringBuilder output) {
        try (var reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[2048];
            int read;
            int retained = 0;
            while ((read = reader.read(buffer)) != -1) {
                if (retained < properties.maxOutputChars()) {
                    int toAppend = Math.min(read, properties.maxOutputChars() - retained);
                    output.append(buffer, 0, toAppend);
                    retained += toAppend;
                }
            }
        } catch (IOException ignored) {
            // Process termination can close the stream while the reader thread is draining it.
        }
    }

    private String limited(String output) {
        if (output.length() <= properties.maxOutputChars()) return output;
        return output.substring(0, properties.maxOutputChars()) + "\n...[saída truncada]";
    }

    private long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup of an isolated temp directory.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
