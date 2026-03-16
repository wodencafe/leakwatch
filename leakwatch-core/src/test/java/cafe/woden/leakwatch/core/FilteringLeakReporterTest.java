package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilteringLeakReporterTest {
    @Test
    void excludingTypesDropsMatchingReports() {
        InMemoryLeakReporter delegate = new InMemoryLeakReporter();
        FilteringLeakReporter reporter = FilteringLeakReporter.excludingTypes(
            delegate,
            Set.of(LeakReportType.STRICT_MODE_WARNING)
        );

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP, "example.TrackedThing"));
        reporter.report(sampleReport(2L, LeakReportType.STRICT_MODE_WARNING, "example.TrackedThing"));

        assertEquals(1, delegate.snapshot().size());
        assertEquals(LeakReportType.GC_WITHOUT_CLEANUP, delegate.snapshot().get(0).type());
        assertEquals(1L, reporter.rejectedReports());
    }

    @Test
    void includingOnlyTypesKeepsOnlyAllowedReports() {
        InMemoryLeakReporter delegate = new InMemoryLeakReporter();
        FilteringLeakReporter reporter = FilteringLeakReporter.includingOnlyTypes(
            delegate,
            Set.of(LeakReportType.FALLBACK_CLEANUP_FAILED)
        );

        reporter.report(sampleReport(3L, LeakReportType.GC_WITHOUT_CLEANUP, "example.TrackedThing"));
        reporter.report(sampleReport(4L, LeakReportType.FALLBACK_CLEANUP_FAILED, "example.TrackedThing"));

        assertEquals(1, delegate.snapshot().size());
        assertEquals(LeakReportType.FALLBACK_CLEANUP_FAILED, delegate.snapshot().get(0).type());
        assertEquals(1L, reporter.rejectedReports());
    }

    private static LeakReport sampleReport(long id, LeakReportType type, String className) {
        return new LeakReport(
            type,
            id,
            className,
            Set.of("close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(123L),
            Set.of("sample"),
            null,
            "sample message",
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "example.DeletePathFallbackCleanup" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "java.io.IOException" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "boom" : null
        );
    }
}
