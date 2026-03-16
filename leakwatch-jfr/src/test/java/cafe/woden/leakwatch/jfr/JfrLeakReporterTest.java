package cafe.woden.leakwatch.jfr;

import cafe.woden.leakwatch.annotations.Severity;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import org.junit.jupiter.api.Test;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JfrLeakReporterTest {
    @Test
    void reporterCommitsAllLeakWatchEventTypes() throws Exception {
        Path recordingFile = Files.createTempFile("leakwatch-jfr-test", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.enable("cafe.woden.leakwatch.RetentionCountExceeded");
            recording.enable("cafe.woden.leakwatch.RetentionApproxBytesExceeded");
            recording.enable("cafe.woden.leakwatch.FallbackCleanupExecuted");
            recording.enable("cafe.woden.leakwatch.FallbackCleanupFailed");
            recording.enable("cafe.woden.leakwatch.RetainedAfterCleanup");
            recording.enable("cafe.woden.leakwatch.StrictModeWarning");
            recording.start();

            JfrLeakReporter reporter = new JfrLeakReporter();
            reporter.report(report(LeakReportType.GC_WITHOUT_CLEANUP, 7L, "example.GcLeak", Set.of("close"), Set.of("lifecycle"), "gc leak", null, null, null));
            reporter.report(report(LeakReportType.RETENTION_COUNT_EXCEEDED, -1L, "example.RetentionLeak", Set.of(), Set.of("retention"), "retention leak", null, null, null));
            reporter.report(retentionBytesReport());
            reporter.report(report(LeakReportType.FALLBACK_CLEANUP_EXECUTED, 8L, "example.FallbackLeak", Set.of("close"), Set.of("fallback"), "fallback cleanup", "example.DeletePathFallbackCleanup", null, null));
            reporter.report(report(LeakReportType.FALLBACK_CLEANUP_FAILED, 9L, "example.BrokenFallbackLeak", Set.of("close"), Set.of("fallback", "failure"), "fallback cleanup failed", "example.DeletePathFallbackCleanup", "java.lang.IllegalStateException", "kaboom"));
            reporter.report(retainedAfterCleanupReport());
            reporter.report(report(LeakReportType.STRICT_MODE_WARNING, -1L, "example.StrictWarning", Set.of(), Set.of("strict"), "strict warning", null, null, null));

            recording.stop();
            recording.dump(recordingFile);
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
        Set<String> eventNames = events.stream()
            .map(event -> event.getEventType().getName())
            .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(
            "cafe.woden.leakwatch.LeakGcWithoutCleanup",
            "cafe.woden.leakwatch.RetentionCountExceeded",
            "cafe.woden.leakwatch.RetentionApproxBytesExceeded",
            "cafe.woden.leakwatch.FallbackCleanupExecuted",
            "cafe.woden.leakwatch.FallbackCleanupFailed",
            "cafe.woden.leakwatch.RetainedAfterCleanup",
            "cafe.woden.leakwatch.StrictModeWarning"
        ), eventNames);

        RecordedEvent gcEvent = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.LeakGcWithoutCleanup"))
            .findFirst()
            .orElseThrow();

        assertEquals(7L, gcEvent.getLong("leakId"));
        assertEquals("example.GcLeak", gcEvent.getString("className"));
        assertEquals("close", gcEvent.getString("expectedCleanupMethods"));
        assertEquals("lifecycle", gcEvent.getString("tags"));
        assertEquals("gc leak", gcEvent.getString("message"));
        assertEquals("WARN", gcEvent.getString("severity"));
        assertTrue(gcEvent.getLong("ageMillis") >= 0L);

        RecordedEvent retentionBytesEvent = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.RetentionApproxBytesExceeded"))
            .findFirst()
            .orElseThrow();

        assertEquals("ERROR", retentionBytesEvent.getString("severity"));
        assertEquals(7L, retentionBytesEvent.getLong("retentionLiveCount"));
        assertEquals(8192L, retentionBytesEvent.getLong("retentionApproxShallowBytes"));
        assertEquals(4096L, retentionBytesEvent.getLong("retentionMaxApproxShallowBytes"));
        assertEquals(4096L, retentionBytesEvent.getLong("retentionApproxBytesOverBudget"));

        RecordedEvent fallbackEvent = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.FallbackCleanupExecuted"))
            .findFirst()
            .orElseThrow();

        assertEquals(8L, fallbackEvent.getLong("leakId"));
        assertEquals("example.FallbackLeak", fallbackEvent.getString("className"));
        assertEquals("fallback", fallbackEvent.getString("tags"));
        assertEquals("fallback cleanup", fallbackEvent.getString("message"));
        assertEquals("INFO", fallbackEvent.getString("severity"));
        assertEquals("example.DeletePathFallbackCleanup", fallbackEvent.getString("fallbackActionClassName"));
        assertEquals("", fallbackEvent.getString("failureClassName"));
        assertEquals("", fallbackEvent.getString("failureMessage"));

        RecordedEvent failedFallbackEvent = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.FallbackCleanupFailed"))
            .findFirst()
            .orElseThrow();

        assertEquals(9L, failedFallbackEvent.getLong("leakId"));
        assertEquals("example.BrokenFallbackLeak", failedFallbackEvent.getString("className"));
        assertEquals("failure,fallback", failedFallbackEvent.getString("tags"));
        assertEquals("fallback cleanup failed", failedFallbackEvent.getString("message"));
        assertEquals("ERROR", failedFallbackEvent.getString("severity"));
        assertEquals("example.DeletePathFallbackCleanup", failedFallbackEvent.getString("fallbackActionClassName"));
        assertEquals("java.lang.IllegalStateException", failedFallbackEvent.getString("failureClassName"));
        assertEquals("kaboom", failedFallbackEvent.getString("failureMessage"));

        RecordedEvent retainedAfterCleanupEvent = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.RetainedAfterCleanup"))
            .findFirst()
            .orElseThrow();

        assertEquals(11L, retainedAfterCleanupEvent.getLong("leakId"));
        assertEquals("example.RetainedSession", retainedAfterCleanupEvent.getString("className"));
        assertEquals("close", retainedAfterCleanupEvent.getString("observedCleanupMethod"));
        assertEquals("WARN", retainedAfterCleanupEvent.getString("severity"));
        assertEquals("2026-03-16T00:00:01Z", retainedAfterCleanupEvent.getString("cleanedAt"));
        assertEquals(250L, retainedAfterCleanupEvent.getLong("ageSinceCleanupMillis"));
        assertEquals(100L, retainedAfterCleanupEvent.getLong("postCleanupGraceMillis"));
    }

    private static LeakReport report(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        Set<String> tags,
        String message,
        String fallbackActionClassName,
        String failureClassName,
        String failureMessage
    ) {
        return new LeakReport(
            type,
            id,
            className,
            expectedCleanupMethods,
            null,
            Instant.now(),
            Duration.ofMillis(25L),
            tags,
            null,
            message,
            fallbackActionClassName,
            failureClassName,
            failureMessage
        );
    }

    private static LeakReport retentionBytesReport() {
        return new LeakReport(
            LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED,
            -1L,
            "example.RetentionLeak",
            Set.of(),
            null,
            Instant.now(),
            Duration.ZERO,
            Set.of("retention", "bytes"),
            null,
            "retention bytes exceeded",
            Severity.ERROR,
            7L,
            5L,
            8192L,
            4096L,
            4096L
        );
    }

    private static LeakReport retainedAfterCleanupReport() {
        return new LeakReport(
            LeakReportType.RETAINED_AFTER_CLEANUP,
            11L,
            "example.RetainedSession",
            Set.of("close"),
            "close",
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofSeconds(1L),
            Set.of("post-cleanup"),
            null,
            "retained after cleanup",
            Severity.WARN,
            Instant.parse("2026-03-16T00:00:01Z"),
            Duration.ofMillis(250L),
            new IllegalStateException("cleanup site"),
            100L
        );
    }
}
