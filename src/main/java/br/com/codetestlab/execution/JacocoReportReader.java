package br.com.codetestlab.execution;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class JacocoReportReader {

    public ExecutionResult enrich(Path reportFile, ExecutionResult fallback) {
        if (reportFile == null || !Files.isRegularFile(reportFile)) {
            return fallback;
        }
        try {
            CoverageResult coverage = read(reportFile);
            return new ExecutionResult(
                    fallback.status(),
                    fallback.testsRun(),
                    fallback.testsPassed(),
                    fallback.testsFailed(),
                    fallback.testsSkipped(),
                    fallback.durationMs(),
                    fallback.output(),
                    coverage);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    CoverageResult read(Path xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Element root = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
        Map<String, Double> percentages = new HashMap<>();

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && "counter".equals(element.getTagName())) {
                String type = element.getAttribute("type");
                percentages.put(type, percentage(element));
            }
        }

        return new CoverageResult(
                percentages.get("LINE"),
                percentages.get("METHOD"),
                percentages.get("BRANCH"),
                percentages.get("CLASS"));
    }

    private Double percentage(Element counter) {
        long missed = longAttribute(counter, "missed");
        long covered = longAttribute(counter, "covered");
        long total = missed + covered;
        if (total == 0) {
            return null;
        }
        double value = (covered * 100.0) / total;
        return Math.round(value * 10.0) / 10.0;
    }

    private long longAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? 0L : Long.parseLong(value);
    }
}
