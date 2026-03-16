package cafe.woden.leakwatch.sample.fallback;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackCleanupIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @Test
    void explicitCleanupPreventsLeakAndFallbackReports() throws Exception {
        TrackedTempFile tracked = new TrackedTempFile("leakwatch-explicit");
        Path path = tracked.path();
        tracked.close();

        assertFalse(Files.exists(path));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void leakedTrackedTempFileRunsDetachedFallbackCleanup() throws Exception {
        LeakedTrackedTempFile leaked = createLeakedTrackedTempFile("leakwatch-fallback");
        WeakReference<TrackedTempFile> reference = leaked.reference();
        Path path = leaked.path();

        LeakReport leakReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP,
            Duration.ofSeconds(10)
        );
        LeakReport fallbackReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.FALLBACK_CLEANUP_EXECUTED,
            Duration.ofSeconds(10)
        );

        GcAwaiter.awaitCondition(
            Duration.ofSeconds(10),
            () -> !Files.exists(path),
            () -> "Timed out waiting for fallback cleanup to delete temp file: " + path
        );

        assertEquals(TrackedTempFile.class.getName(), leakReport.className());
        assertEquals(TrackedTempFile.class.getName(), fallbackReport.className());
        assertTrue(fallbackReport.message().contains(DeletePathFallbackCleanup.class.getName()));

        List<LeakReport> reports = reporter.snapshot();
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP));
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.FALLBACK_CLEANUP_EXECUTED));
    }

    private LeakedTrackedTempFile createLeakedTrackedTempFile(String prefix) throws Exception {
        TrackedTempFile leaked = new TrackedTempFile(prefix);
        return new LeakedTrackedTempFile(new WeakReference<>(leaked), leaked.path());
    }

    private record LeakedTrackedTempFile(WeakReference<TrackedTempFile> reference, Path path) {
    }
}
