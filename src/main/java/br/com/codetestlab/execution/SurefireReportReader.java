package br.com.codetestlab.execution;

import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SurefireReportReader {
    public ExecutionResult enrich(Path reportsDirectory, ExecutionResult fallback) {
        if (reportsDirectory == null || !Files.isDirectory(reportsDirectory)) return fallback;

        int tests = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        int files = 0;

        try (var paths = Files.list(reportsDirectory)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().startsWith("TEST-")
                    && p.getFileName().toString().endsWith(".xml")).toList()) {
                Element suite = parse(path);
                tests += intAttribute(suite, "tests");
                failures += intAttribute(suite, "failures");
                errors += intAttribute(suite, "errors");
                skipped += intAttribute(suite, "skipped");
                files++;
            }
        } catch (Exception ignored) {
            return fallback;
        }

        if (files == 0) return fallback;
        int failed = failures + errors;
        int passed = Math.max(0, tests - failed - skipped);
        return new ExecutionResult(fallback.status(), tests, passed, failed, skipped,
                fallback.durationMs(), fallback.output());
    }

    private Element parse(Path xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
    }

    private int intAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }
}
