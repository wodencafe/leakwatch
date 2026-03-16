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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollingLeakReportJsonFileReporterTest {
    @TempDir
    Path tempDir;

    @Test
    void rotatesCurrentFileIntoArchivesWhenMaxBytesWouldBeExceeded() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");
        RollingLeakReportJsonFileReporter reporter = new RollingLeakReportJsonFileReporter(output, 1L, 2);

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP));
        reporter.report(sampleReport(2L, LeakReportType.FALLBACK_CLEANUP_EXECUTED));
        reporter.report(sampleReport(3L, LeakReportType.FALLBACK_CLEANUP_FAILED));

        List<String> currentLines = Files.readAllLines(output);
        List<String> archiveOneLines = Files.readAllLines(output.resolveSibling("leakwatch.ndjson.1"));
        List<String> archiveTwoLines = Files.readAllLines(output.resolveSibling("leakwatch.ndjson.2"));

        assertEquals(1, currentLines.size());
        assertEquals(1, archiveOneLines.size());
        assertEquals(1, archiveTwoLines.size());

        assertTrue(currentLines.get(0).contains("\"id\":3"));
        assertTrue(archiveOneLines.get(0).contains("\"id\":2"));
        assertTrue(archiveTwoLines.get(0).contains("\"id\":1"));
    }

    @Test
    void canRotateWithoutKeepingArchives() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");
        RollingLeakReportJsonFileReporter reporter = new RollingLeakReportJsonFileReporter(output, 1L, 0);

        reporter.report(sampleReport(10L, LeakReportType.GC_WITHOUT_CLEANUP));
        reporter.report(sampleReport(11L, LeakReportType.STRICT_MODE_WARNING));

        List<String> currentLines = Files.readAllLines(output);
        assertEquals(1, currentLines.size());
        assertTrue(currentLines.get(0).contains("\"id\":11"));
        assertFalse(Files.exists(output.resolveSibling("leakwatch.ndjson.1")));
    }

    @Test
    void wrapsWriteFailuresInUncheckedIOException() throws Exception {
        Path output = tempDir.resolve("already-a-directory");
        Files.createDirectories(output);

        RollingLeakReportJsonFileReporter reporter = new RollingLeakReportJsonFileReporter(output, 1024L, 1);

        UncheckedIOException thrown = assertThrows(
            UncheckedIOException.class,
            () -> reporter.report(sampleReport(12L, LeakReportType.STRICT_MODE_WARNING))
        );

        assertTrue(thrown.getMessage().contains(output.toString()));
    }

    private static LeakReport sampleReport(long id, LeakReportType type) {
        return new LeakReport(
            type,
            id,
            "example.RollingTrackedThing",
            Set.of("close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(123L),
            Set.of("rolling", "json"),
            null,
            "sample message",
            type == LeakReportType.FALLBACK_CLEANUP_FAILED || type == LeakReportType.FALLBACK_CLEANUP_EXECUTED
                ? "example.DeletePathFallbackCleanup"
                : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "java.io.IOException" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "boom" : null
        );
    }
}
