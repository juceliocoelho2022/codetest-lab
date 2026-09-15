import br.com.codetestlab.execution.DockerCommandBuilder;
import br.com.codetestlab.execution.ExecutionOutputParser;
import br.com.codetestlab.execution.ExecutionStatus;
import br.com.codetestlab.execution.SafeZipExtractor;
import br.com.codetestlab.execution.SurefireReportReader;
import br.com.codetestlab.execution.ExecutionResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CoreSmokeTest {
    public static void main(String[] args) throws Exception {
        var parser = new ExecutionOutputParser();
        var passed = parser.parse(0, "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0", 15);
        check(passed.status() == ExecutionStatus.PASSED, "parser should mark exit 0 as PASSED");
        check(passed.testsRun() == 2 && passed.testsPassed() == 2, "parser should count passing tests");

        var failed = parser.parse(1, "Tests run: 3, Failures: 1, Errors: 0, Skipped: 0", 20);
        check(failed.status() == ExecutionStatus.FAILED, "parser should mark test failure");
        check(failed.testsFailed() == 1, "parser should count failed tests");

        List<String> command = new DockerCommandBuilder().build(Path.of("/tmp/work"), "codetest-lab-runner:latest");
        check(command.contains("--network") && command.contains("none"), "docker command must disable network");
        check(command.contains("--read-only"), "docker command must use a read-only root filesystem");
        check(command.contains("--cap-drop") && command.contains("ALL"), "docker command must drop capabilities");
        check(command.stream().anyMatch(v -> v.contains("no-new-privileges")), "docker command must set no-new-privileges");
        check(command.contains("/tmp:rw,noexec,nosuid,size=64m"), "general /tmp must remain noexec");
        check(command.contains("/jansi-tmp:rw,exec,nosuid,nodev,size=8m"), "Jansi must get a dedicated executable tmpfs");
        check(command.contains("MAVEN_OPTS=-Djansi.tmpdir=/jansi-tmp -Djansi.mode=strip"), "Maven must use the dedicated Jansi tmpdir");

        Path dest = Files.createTempDirectory("zip-smoke");
        byte[] zip = zipOf("src/main/java/demo/Hello.java", "package demo; public class Hello {}\n");
        new SafeZipExtractor(200, 2_000_000).extractJavaSources(zip, dest);
        check(Files.exists(dest.resolve("demo/Hello.java")), "zip extractor should preserve Java source path");

        boolean traversalRejected = false;
        try {
            new SafeZipExtractor(200, 2_000_000).extractJavaSources(zipOf("../evil.java", "class Evil {}"), dest);
        } catch (IllegalArgumentException expected) {
            traversalRejected = true;
        }
        check(traversalRejected, "zip extractor must reject traversal");

        Path reports = Files.createTempDirectory("surefire-smoke");
        Files.writeString(reports.resolve("TEST-demo.xml"),
                "<testsuite tests=\"4\" failures=\"1\" errors=\"0\" skipped=\"1\"></testsuite>");
        ExecutionResult enriched = new SurefireReportReader().enrich(reports, failed);
        check(enriched.testsRun() == 4, "surefire reader should read total tests");
        check(enriched.testsPassed() == 2, "surefire reader should calculate passed tests");

        System.out.println("CORE_SMOKE_OK");
    }

    private static byte[] zipOf(String name, String content) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
