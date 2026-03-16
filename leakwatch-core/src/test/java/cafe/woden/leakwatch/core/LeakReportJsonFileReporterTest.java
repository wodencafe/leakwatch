package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakReportJsonFileReporterTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsNdjsonReportsAndCreatesParentDirectories() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");
        LeakReportJsonFileReporter reporter = new LeakReportJsonFileReporter(output);

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP));
        reporter.report(sampleReport(2L, LeakReportType.FALLBACK_CLEANUP_FAILED));

        List<String> lines = Files.readAllLines(output);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"id\":1"));
        assertTrue(lines.get(0).contains("\"type\":\"GC_WITHOUT_CLEANUP\""));
        assertTrue(lines.get(1).contains("\"id\":2"));
        assertTrue(lines.get(1).contains("\"type\":\"FALLBACK_CLEANUP_FAILED\""));
        assertTrue(lines.get(1).contains("\"failureClassName\":\"java.io.IOException\""));
    }

    @Test
    void wrapsWriteFailuresInUncheckedIOException() throws Exception {
        Path output = tempDir.resolve("already-a-directory");
        Files.createDirectories(output);

        LeakReportJsonFileReporter reporter = new LeakReportJsonFileReporter(output);

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> reporter.report(sampleReport(3L, LeakReportType.STRICT_MODE_WARNING))
        );

        assertTrue(thrown.getMessage().contains(output.toString()));
    }

    private static LeakReport sampleReport(long id, LeakReportType type) {
        return new LeakReport(
            type,
            id,
            "example.TrackedThing",
            Set.of("close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(123L),
            Set.of("sample", "json"),
            null,
            "sample message",
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "example.DeletePathFallbackCleanup" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "java.io.IOException" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "boom" : null
        );
    }
}
