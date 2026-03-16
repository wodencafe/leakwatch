package cafe.woden.leakwatch.sample.observability;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportJsonFileReporter;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.metrics.LeakMetricsSnapshot;
import cafe.woden.leakwatch.metrics.LeakReportMetrics;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityIntegrationTest {
    @TempDir
    Path tempDir;

    private InMemoryLeakReporter inMemory;
    private LeakReportMetrics metrics;
    private Path outputFile;

    @BeforeEach
    void setUp() {
        inMemory = new InMemoryLeakReporter();
        metrics = new LeakReportMetrics();
        outputFile = tempDir.resolve("reports/leakwatch.ndjson");
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                inMemory,
                metrics,
                new LeakReportJsonFileReporter(outputFile)
            )
        );
    }

    @Test
    void leakedTrackedObjectUpdatesMetricsAndWritesNdjsonReport() throws Exception {
        WeakReference<ObservableSampleResource> reference = createLeakedResource();

        LeakReport report = GcAwaiter.awaitReport(
            reference,
            inMemory::snapshot,
            candidate -> candidate.type() == LeakReportType.GC_WITHOUT_CLEANUP,
            Duration.ofSeconds(10)
        );

        LeakMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(1L, snapshot.totalReports());
        assertEquals(1L, snapshot.countsByType().get(LeakReportType.GC_WITHOUT_CLEANUP));
        assertEquals(1L, snapshot.countsByClass().get(ObservableSampleResource.class.getName()));

        GcAwaiter.awaitCondition(
            Duration.ofSeconds(5),
            () -> Files.exists(outputFile),
            () -> "Expected JSON output file to exist: " + outputFile
        );

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"type\":\"GC_WITHOUT_CLEANUP\""));
        assertTrue(lines.get(0).contains("\"className\":\"" + ObservableSampleResource.class.getName() + "\""));
        assertTrue(report.message().contains("garbage collected before an explicit cleanup method"));
    }

    private WeakReference<ObservableSampleResource> createLeakedResource() {
        ObservableSampleResource leaked = new ObservableSampleResource("leaked-observability-test");
        return new WeakReference<>(leaked);
    }
}
