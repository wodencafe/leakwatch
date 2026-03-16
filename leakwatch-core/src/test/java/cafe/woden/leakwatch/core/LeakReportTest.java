package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.Severity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeakReportTest {
    @Test
    void canonicalizesExpectedCleanupMethodsAndTagsForStableReporting() {
        LeakReport report = new LeakReport(
            LeakReportType.FALLBACK_CLEANUP_FAILED,
            42L,
            "example.BrokenFallback",
            new LinkedHashSet<>(java.util.List.of("dispose", "close", "abort")),
            null,
            Instant.now(),
            Duration.ofMillis(15L),
            new LinkedHashSet<>(java.util.List.of("failure", "fallback", "alpha")),
            null,
            "boom"
        );

        assertIterableEquals(java.util.List.of("abort", "close", "dispose"), report.expectedCleanupMethods());
        assertIterableEquals(java.util.List.of("alpha", "failure", "fallback"), report.tags());
        assertEquals("abort,close,dispose", report.expectedCleanupMethodsCsv());
        assertEquals("alpha,failure,fallback", report.tagsCsv());
        assertEquals(Severity.ERROR, report.severity());
    }

    @Test
    void handlesNullAndEmptyCollectionsWhenBuildingCsvValues() {
        LeakReport report = new LeakReport(
            LeakReportType.STRICT_MODE_WARNING,
            -1L,
            "example.StrictWarning",
            null,
            null,
            Instant.now(),
            Duration.ZERO,
            Set.of(),
            null,
            "warning"
        );

        assertEquals(Set.of(), report.expectedCleanupMethods());
        assertEquals(Set.of(), report.tags());
        assertEquals("", report.expectedCleanupMethodsCsv());
        assertEquals("", report.tagsCsv());
        assertNull(report.fallbackActionClassName());
        assertNull(report.failureClassName());
        assertNull(report.failureMessage());
        assertNull(report.retentionLiveCount());
        assertNull(report.cleanedAt());
        assertNull(report.ageSinceCleanup());
        assertNull(report.cleanupSite());
        assertNull(report.postCleanupGraceMillis());
    }

    @Test
    void preservesStructuredFallbackMetadataWhenProvided() {
        LeakReport report = new LeakReport(
            LeakReportType.FALLBACK_CLEANUP_FAILED,
            99L,
            "example.BrokenFallback",
            Set.of("close"),
            null,
            Instant.now(),
            Duration.ofMillis(5L),
            Set.of("fallback", "failure"),
            null,
            "fallback cleanup failed",
            "example.DeletePathFallbackCleanup",
            "java.lang.IllegalStateException",
            "kaboom"
        );

        assertEquals("example.DeletePathFallbackCleanup", report.fallbackActionClassName());
        assertEquals("java.lang.IllegalStateException", report.failureClassName());
        assertEquals("kaboom", report.failureMessage());
    }

    @Test
    void preservesStructuredRetentionMetadataWhenProvided() {
        LeakReport report = new LeakReport(
            LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED,
            -1L,
            "example.RetentionThing",
            Set.of(),
            null,
            Instant.now(),
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

        assertEquals(Severity.ERROR, report.severity());
        assertEquals(7L, report.retentionLiveCount());
        assertEquals(5L, report.retentionMaxLiveInstances());
        assertEquals(8192L, report.retentionApproxShallowBytes());
        assertEquals(4096L, report.retentionMaxApproxShallowBytes());
        assertEquals(4096L, report.retentionApproxBytesOverBudget());
    }

    @Test
    void preservesStructuredPostCleanupMetadataWhenProvided() {
        Instant cleanedAt = Instant.parse("2026-03-16T00:00:01Z");
        Duration ageSinceCleanup = Duration.ofMillis(250L);
        Throwable cleanupSite = new IllegalStateException("cleanup site");

        LeakReport report = new LeakReport(
            LeakReportType.RETAINED_AFTER_CLEANUP,
            5L,
            "example.SocketHandle",
            Set.of("close"),
            "close",
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofSeconds(1L),
            Set.of("post-cleanup"),
            new IllegalStateException("allocation site"),
            "retained after cleanup",
            Severity.WARN,
            cleanedAt,
            ageSinceCleanup,
            cleanupSite,
            100L
        );

        assertEquals(cleanedAt, report.cleanedAt());
        assertEquals(ageSinceCleanup, report.ageSinceCleanup());
        assertEquals(cleanupSite, report.cleanupSite());
        assertEquals(100L, report.postCleanupGraceMillis());
    }
}
