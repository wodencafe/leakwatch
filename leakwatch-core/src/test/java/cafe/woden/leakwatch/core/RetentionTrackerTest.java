package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.RetentionSuspect;
import cafe.woden.leakwatch.annotations.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetentionTrackerTest {
    @Test
    void emitsApproxByteBudgetReportWhenConfiguredAndEstimatorAvailable() {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        RetentionTracker tracker = new RetentionTracker(
            reporter,
            LeakWatchConfig.defaults(),
            new StartupDiagnostics(),
            instance -> 128L
        );

        RetentionSuspect annotation = ByteBudgetTracked.class.getAnnotation(RetentionSuspect.class);
        tracker.track(new ByteBudgetTracked(), ByteBudgetTracked.class, annotation);
        tracker.track(new ByteBudgetTracked(), ByteBudgetTracked.class, annotation);
        tracker.track(new ByteBudgetTracked(), ByteBudgetTracked.class, annotation);

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED)
            .toList();

        assertEquals(1, reports.size());
        LeakReport report = reports.get(0);
        assertEquals(Severity.ERROR, report.severity());
        assertEquals(3L, report.retentionLiveCount());
        assertEquals(384L, report.retentionApproxShallowBytes());
        assertEquals(256L, report.retentionMaxApproxShallowBytes());
        assertEquals(128L, report.retentionApproxBytesOverBudget());
    }

    @Test
    void warnsOnceInStrictModeWhenByteBudgetIsConfiguredWithoutEstimator() {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchConfig defaults = LeakWatchConfig.defaults();
        LeakWatchConfig strictConfig = new LeakWatchConfig(
            defaults.enabled(),
            defaults.defaultCaptureStackTraces(),
            true,
            defaults.defaultConventionCleanupDetection(),
            defaults.excludedPackages()
        );
        RetentionTracker tracker = new RetentionTracker(
            reporter,
            strictConfig,
            new StartupDiagnostics(),
            ShallowSizeEstimator.unsupported()
        );

        RetentionSuspect annotation = ByteBudgetTracked.class.getAnnotation(RetentionSuspect.class);
        tracker.track(new ByteBudgetTracked(), ByteBudgetTracked.class, annotation);
        tracker.track(new ByteBudgetTracked(), ByteBudgetTracked.class, annotation);

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.STRICT_MODE_WARNING)
            .toList();

        assertEquals(1, reports.size());
        assertEquals(ByteBudgetTracked.class.getName(), reports.get(0).className());
    }

    @RetentionSuspect(maxApproxShallowBytes = 256, severity = Severity.ERROR, tags = {"retention", "bytes"})
    private static final class ByteBudgetTracked {
        private final byte[] payload = new byte[64];
    }
}
