package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.Severity;
import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportJsonFileReporter;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoldenPathIntegrationTest {
    @TempDir
    Path tempDir;

    private InMemoryLeakReporter reporter;
    private Path outputFile;

    @BeforeEach
    void setUp() {
        GoldenPathListenerRegistration.clearRetained();
        reporter = new InMemoryLeakReporter();
        outputFile = tempDir.resolve("reports/golden-path.ndjson");
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                reporter,
                new LeakReportJsonFileReporter(outputFile)
            )
        );
    }

    @AfterEach
    void tearDown() {
        GoldenPathListenerRegistration.clearRetained();
    }

    @Test
    void goldenPathCoversExplicitCleanupFallbackPostCleanupRetentionAndJsonReporting() throws Exception {
        assertTrue(ShallowSizeEstimators.autoDiscover().description().contains("leakwatch-jol"));

        try (GoldenPathSession session = new GoldenPathSession("cleaned-test-session")) {
            assertEquals("cleaned-test-session", session.name());
        }

        GoldenPathTempFile leaked = new GoldenPathTempFile("leakwatch-golden-test");
        Path leakedPath = leaked.path();
        WeakReference<GoldenPathTempFile> leakedReference = new WeakReference<>(leaked);
        leaked = null;

        GoldenPathListenerRegistration listener = new GoldenPathListenerRegistration("zombie-listener-test");
        assertEquals("zombie-listener-test", listener.listenerId());
        listener.close();
        listener = null;

        List<GoldenPathCacheEntry> retained = List.of(
            new GoldenPathCacheEntry(),
            new GoldenPathCacheEntry(),
            new GoldenPathCacheEntry()
        );
        assertEquals(3, retained.size());
        assertEquals(4096, retained.get(0).payloadLength());

        LeakReport gcReport = GcAwaiter.awaitReport(
            leakedReference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP
                && report.className().equals(GoldenPathTempFile.class.getName()),
            Duration.ofSeconds(10)
        );
        LeakReport fallbackReport = GcAwaiter.awaitReport(
            leakedReference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.FALLBACK_CLEANUP_EXECUTED
                && report.className().equals(GoldenPathTempFile.class.getName()),
            Duration.ofSeconds(10)
        );

        GcAwaiter.awaitCondition(
            Duration.ofSeconds(5),
            () -> reporter.snapshot().stream().anyMatch(report -> report.type() == LeakReportType.RETAINED_AFTER_CLEANUP
                && report.className().equals(GoldenPathListenerRegistration.class.getName())),
            () -> "Expected retained-after-cleanup report for golden-path listener registration. Reports seen: " + reporter.snapshot()
        );
        GcAwaiter.awaitCondition(
            Duration.ofSeconds(10),
            () -> reporter.snapshot().stream().anyMatch(report -> report.type() == LeakReportType.RETENTION_COUNT_EXCEEDED
                && report.className().equals(GoldenPathCacheEntry.class.getName())),
            () -> "Expected retention count report for golden-path cache entries. Reports seen: " + reporter.snapshot()
        );
        GcAwaiter.awaitCondition(
            Duration.ofSeconds(10),
            () -> reporter.snapshot().stream().anyMatch(report -> report.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED
                && report.className().equals(GoldenPathCacheEntry.class.getName())),
            () -> "Expected retention byte-budget report for golden-path cache entries. Reports seen: " + reporter.snapshot()
        );
        GcAwaiter.awaitCondition(
            Duration.ofSeconds(10),
            () -> !Files.exists(leakedPath),
            () -> "Expected fallback cleanup to delete temp file: " + leakedPath
        );
        GcAwaiter.awaitCondition(
            Duration.ofSeconds(5),
            () -> Files.exists(outputFile),
            () -> "Expected JSON output file to exist: " + outputFile
        );

        List<LeakReport> reports = reporter.snapshot();
        assertFalse(reports.stream().anyMatch(report -> report.className().equals(GoldenPathSession.class.getName())));
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP
            && report.className().equals(GoldenPathTempFile.class.getName())));
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.FALLBACK_CLEANUP_EXECUTED
            && report.className().equals(GoldenPathTempFile.class.getName())));
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.RETAINED_AFTER_CLEANUP
            && report.className().equals(GoldenPathListenerRegistration.class.getName())));

        LeakReport retainedAfterCleanupReport = reports.stream()
            .filter(report -> report.type() == LeakReportType.RETAINED_AFTER_CLEANUP
                && report.className().equals(GoldenPathListenerRegistration.class.getName()))
            .findFirst()
            .orElseThrow();
        LeakReport retentionCountReport = reports.stream()
            .filter(report -> report.type() == LeakReportType.RETENTION_COUNT_EXCEEDED
                && report.className().equals(GoldenPathCacheEntry.class.getName()))
            .findFirst()
            .orElseThrow();
        LeakReport retentionBytesReport = reports.stream()
            .filter(report -> report.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED
                && report.className().equals(GoldenPathCacheEntry.class.getName()))
            .findFirst()
            .orElseThrow();

        assertEquals(GoldenPathTempFile.class.getName(), gcReport.className());
        assertEquals(GoldenPathTempFile.class.getName(), fallbackReport.className());
        assertEquals(Severity.WARN, retainedAfterCleanupReport.severity());
        assertEquals("close", retainedAfterCleanupReport.observedCleanupMethodName());
        assertTrue(retainedAfterCleanupReport.tags().contains("post-cleanup"));
        assertNotNull(retainedAfterCleanupReport.allocationSite());
        assertNotNull(retainedAfterCleanupReport.cleanedAt());
        assertNotNull(retainedAfterCleanupReport.ageSinceCleanup());
        assertNotNull(retainedAfterCleanupReport.cleanupSite());
        assertEquals(Long.valueOf(150L), retainedAfterCleanupReport.postCleanupGraceMillis());
        assertNotNull(retentionCountReport.retentionLiveCount());
        assertEquals(Long.valueOf(1L), retentionCountReport.retentionMaxLiveInstances());
        assertTrue(retentionCountReport.retentionLiveCount() > retentionCountReport.retentionMaxLiveInstances(),
            () -> "Expected retention count report to capture a threshold crossing snapshot. Report was: " + retentionCountReport);
        assertEquals(Severity.ERROR, retentionBytesReport.severity());
        assertNotNull(retentionBytesReport.retentionApproxShallowBytes());
        assertNotNull(retentionBytesReport.retentionApproxBytesOverBudget());
        assertTrue(retentionBytesReport.retentionApproxShallowBytes() > retentionBytesReport.retentionMaxApproxShallowBytes());

        List<String> lines = Files.readAllLines(outputFile);
        assertTrue(lines.size() >= 5, "Expected at least 5 NDJSON lines but saw " + lines.size());
        assertTrue(lines.stream().anyMatch(line -> line.contains("\"type\":\"GC_WITHOUT_CLEANUP\"")
            && line.contains("\"className\":\"" + GoldenPathTempFile.class.getName() + "\"")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("\"type\":\"FALLBACK_CLEANUP_EXECUTED\"")
            && line.contains("\"className\":\"" + GoldenPathTempFile.class.getName() + "\"")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("\"type\":\"RETAINED_AFTER_CLEANUP\"")
            && line.contains("\"className\":\"" + GoldenPathListenerRegistration.class.getName() + "\"")
            && line.contains("\"postCleanupGraceMillis\":150")
            && line.contains("\"allocationSite\":")
            && line.contains("\"cleanupSite\":")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("\"type\":\"RETENTION_COUNT_EXCEEDED\"")
            && line.contains("\"className\":\"" + GoldenPathCacheEntry.class.getName() + "\"")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("\"type\":\"RETENTION_APPROX_BYTES_EXCEEDED\"")
            && line.contains("\"className\":\"" + GoldenPathCacheEntry.class.getName() + "\"")));
    }
}
