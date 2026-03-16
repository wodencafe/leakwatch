package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.Severity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakReportJsonFormatterTest {
    @Test
    void formatsReportAsStableJson() {
        LeakReport report = new LeakReport(
            LeakReportType.FALLBACK_CLEANUP_FAILED,
            42L,
            "example.TrackedThing",
            Set.of("dispose", "close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(1234),
            Set.of("fallback", "failure"),
            null,
            "Detached fallback cleanup action failed after GC-before-cleanup.",
            "example.FallbackAction",
            "java.io.IOException",
            "simulated fallback failure for /tmp/demo.txt"
        );

        String json = LeakReportJsonFormatter.toJson(report);

        assertTrue(json.contains("\"type\":\"FALLBACK_CLEANUP_FAILED\""));
        assertTrue(json.contains("\"severity\":\"ERROR\""));
        assertTrue(json.contains("\"id\":42"));
        assertTrue(json.contains("\"className\":\"example.TrackedThing\""));
        assertTrue(json.contains("\"expectedCleanupMethods\":[\"close\",\"dispose\"]"));
        assertTrue(json.contains("\"tags\":[\"failure\",\"fallback\"]"));
        assertTrue(json.contains("\"fallbackActionClassName\":\"example.FallbackAction\""));
        assertTrue(json.contains("\"failureClassName\":\"java.io.IOException\""));
        assertTrue(json.contains("\"failureMessage\":\"simulated fallback failure for /tmp/demo.txt\""));
        assertTrue(json.contains("\"allocationSite\":null"));
    }

    @Test
    void escapesSpecialCharacters() {
        Throwable allocationSite = new IllegalStateException("line1\nline2");
        LeakReport report = new LeakReport(
            LeakReportType.STRICT_MODE_WARNING,
            7L,
            "example.Quoted\\Thing",
            Set.of(),
            "clean\\up\"method",
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ZERO,
            Set.of("tab\tvalue"),
            allocationSite,
            "quoted \"message\"",
            null,
            null,
            null
        );

        String json = LeakReportJsonFormatter.toJson(report);

        assertTrue(json.contains("example.Quoted\\\\Thing"));
        assertTrue(json.contains("clean\\\\up\\\"method"));
        assertTrue(json.contains("tab\\tvalue"));
        assertTrue(json.contains("quoted \\\"message\\\""));
        assertTrue(json.contains("IllegalStateException: line1\\nline2"));
    }

    @Test
    void writesStructuredRetentionFields() {
        LeakReport report = new LeakReport(
            LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED,
            -1L,
            "example.RetentionThing",
            Set.of(),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ZERO,
            Set.of("retention"),
            null,
            "retention bytes exceeded",
            Severity.ERROR,
            7L,
            5L,
            8192L,
            4096L,
            4096L
        );

        String json = LeakReportJsonFormatter.toJson(report);

        assertTrue(json.contains("\"severity\":\"ERROR\""));
        assertTrue(json.contains("\"retentionLiveCount\":7"));
        assertTrue(json.contains("\"retentionMaxLiveInstances\":5"));
        assertTrue(json.contains("\"retentionApproxShallowBytes\":8192"));
        assertTrue(json.contains("\"retentionMaxApproxShallowBytes\":4096"));
        assertTrue(json.contains("\"retentionApproxBytesOverBudget\":4096"));
    }

    @Test
    void writesStructuredPostCleanupFields() {
        LeakReport report = new LeakReport(
            LeakReportType.RETAINED_AFTER_CLEANUP,
            17L,
            "example.SocketHandle",
            Set.of("close"),
            "close",
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofSeconds(2L),
            Set.of("post-cleanup"),
            new IllegalStateException("allocation site"),
            "retained after cleanup",
            Severity.WARN,
            Instant.parse("2026-03-16T00:00:01Z"),
            Duration.ofMillis(250L),
            new IllegalStateException("cleanup site"),
            100L
        );

        String json = LeakReportJsonFormatter.toJson(report);

        assertTrue(json.contains("\"cleanedAt\":\"2026-03-16T00:00:01Z\""));
        assertTrue(json.contains("\"ageSinceCleanupMillis\":250"));
        assertTrue(json.contains("\"postCleanupGraceMillis\":100"));
        assertTrue(json.contains("\"cleanupSite\":\"java.lang.IllegalStateException: cleanup site"));
    }
}
