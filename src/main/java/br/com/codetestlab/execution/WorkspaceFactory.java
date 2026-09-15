package br.com.codetestlab.execution;

import br.com.codetestlab.config.ExecutionProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorkspaceFactory {
    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");
    private static final Pattern TEST_CLASS = Pattern.compile("(?:public\\s+)?(?:final\\s+)?class\\s+([A-Za-z_$][A-Za-z\\d_$]*)");

    private final ExecutionProperties properties;

    public WorkspaceFactory(ExecutionProperties properties) {
        this.properties = properties;
    }

    public Path create(ExecutionRequest request) throws IOException {
        Path workspace = Files.createTempDirectory("codetest-lab-");
        Files.writeString(
                workspace.resolve("pom.xml"),
                runnerPom(request.executionProfile()),
                StandardCharsets.UTF_8);

        Path mainRoot = Files.createDirectories(workspace.resolve("src/main/java"));
        Path testRoot = Files.createDirectories(workspace.resolve("src/test/java"));

        if (request.isZip()) {
            if (request.zipBytes().length > properties.maxZipBytes()) {
                throw new IllegalArgumentException("O ZIP excede o limite de " + properties.maxZipBytes() + " bytes.");
            }
            new SafeZipExtractor(properties.maxZipEntries(), properties.maxExtractedBytes())
                    .extractJavaSources(request.zipBytes(), mainRoot);
        } else {
            if (request.sourceCode().length() > properties.maxSourceChars()) {
                throw new IllegalArgumentException("O código-fonte excede o limite permitido.");
            }
            String sourceFile = simpleClassName(request.className()) + ".java";
            Files.writeString(mainRoot.resolve(sourceFile), request.sourceCode(), StandardCharsets.UTF_8);
        }

        String testClassName = findTestClassName(request.testCode());
        Files.writeString(testRoot.resolve(testClassName + ".java"), request.testCode(), StandardCharsets.UTF_8);
        return workspace;
    }

    static String simpleClassName(String className) {
        String value = className == null ? "" : className.trim();
        int index = value.lastIndexOf('.');
        String simple = index >= 0 ? value.substring(index + 1) : value;
        if (!JAVA_IDENTIFIER.matcher(simple).matches()) {
            throw new IllegalArgumentException("Nome de classe Java inválido.");
        }
        return simple;
    }

    private String findTestClassName(String testCode) {
        Matcher matcher = TEST_CLASS.matcher(testCode);
        if (!matcher.find()) {
            throw new IllegalArgumentException("O teste deve declarar uma classe Java.");
        }
        return matcher.group(1);
    }

    static String runnerPom() {
        return runnerPom(ExecutionProfile.defaultProfile());
    }

    static String runnerPom(ExecutionProfile executionProfile) {
        ExecutionProfile profile = executionProfile == null
                ? ExecutionProfile.defaultProfile()
                : executionProfile;

        String mockitoDependency = profile.mockitoEnabled() ? """
                        <dependency>
                            <groupId>org.mockito</groupId>
                            <artifactId>mockito-junit-jupiter</artifactId>
                            <version>5.15.2</version>
                            <scope>test</scope>
                        </dependency>
                """ : "";

        String jacocoPlugin = profile.jacocoEnabled() ? """
                            <plugin>
                                <groupId>org.jacoco</groupId>
                                <artifactId>jacoco-maven-plugin</artifactId>
                                <version>0.8.12</version>
                                <executions>
                                    <execution>
                                        <id>prepare-agent</id>
                                        <phase>initialize</phase>
                                        <goals>
                                            <goal>prepare-agent</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>jacoco-report</id>
                                        <phase>verify</phase>
                                        <goals>
                                            <goal>report</goal>
                                        </goals>
                                        <configuration>
                                            <formats>
                                                <format>XML</format>
                                            </formats>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                """ : "";

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>local.codetest</groupId>
                    <artifactId>submission</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <maven.compiler.release>21</maven.compiler.release>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.11.4</version>
                            <scope>test</scope>
                        </dependency>
                %s
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <useModulePath>false</useModulePath>
                                    <trimStackTrace>false</trimStackTrace>
                                </configuration>
                            </plugin>
                %s
                        </plugins>
                    </build>
                </project>
                """.formatted(mockitoDependency, jacocoPlugin);
    }
}
