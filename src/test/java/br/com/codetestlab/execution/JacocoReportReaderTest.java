package br.com.codetestlab.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacocoReportReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadProjectLevelCoverageCounters() throws Exception {
        Path xml = tempDir.resolve("jacoco.xml");
        Files.writeString(xml, """
                <?xml version="1.0" encoding="UTF-8"?>
                <report name="submission">
                  <package name="demo">
                    <counter type="LINE" missed="99" covered="1"/>
                  </package>
                  <counter type="INSTRUCTION" missed="10" covered="90"/>
                  <counter type="BRANCH" missed="2" covered="6"/>
                  <counter type="LINE" missed="2" covered="8"/>
                  <counter type="METHOD" missed="1" covered="3"/>
                  <counter type="CLASS" missed="0" covered="2"/>
                </report>
                """);

        CoverageResult coverage = new JacocoReportReader().read(xml);

        assertEquals(80.0, coverage.linePercent());
        assertEquals(75.0, coverage.methodPercent());
        assertEquals(75.0, coverage.branchPercent());
        assertEquals(100.0, coverage.classPercent());
    }

    @Test
    void shouldReturnNullForZeroDenominatorMetric() throws Exception {
        Path xml = tempDir.resolve("jacoco.xml");
        Files.writeString(xml, """
                <?xml version="1.0" encoding="UTF-8"?>
                <report name="submission">
                  <counter type="LINE" missed="0" covered="0"/>
                  <counter type="METHOD" missed="1" covered="1"/>
                </report>
                """);

        CoverageResult coverage = new JacocoReportReader().read(xml);

        assertNull(coverage.linePercent());
        assertEquals(50.0, coverage.methodPercent());
        assertNull(coverage.branchPercent());
        assertNull(coverage.classPercent());
    }

    @Test
    void shouldRejectDoctypeAndExternalEntityPayloads() throws Exception {
        Path xml = tempDir.resolve("malicious.xml");
        Files.writeString(xml, """
                <?xml version="1.0"?>
                <!DOCTYPE report [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <report name="&xxe;">
                  <counter type="LINE" missed="0" covered="1"/>
                </report>
                """);

        assertThrows(Exception.class, () -> new JacocoReportReader().read(xml));
    }
}
