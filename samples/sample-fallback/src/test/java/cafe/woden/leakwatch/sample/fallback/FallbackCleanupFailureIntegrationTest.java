package cafe.woden.leakwatch.sample.fallback;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackCleanupFailureIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (Path path : reporter.snapshot().stream()
            .filter(report -> report.className().equals(BrokenTrackedTempFile.class.getName()))
            .map(FallbackCleanupFailureIntegrationTest::extractPathSuffix)
            .filter(suffix -> !suffix.isBlank())
            .map(Path::of)
            .toList()) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void leakedTrackedTempFileReportsFallbackFailure() throws Exception {
        LeakedBrokenTrackedTempFile leaked = createLeakedBrokenTrackedTempFile("leakwatch-broken-fallback");
        WeakReference<BrokenTrackedTempFile> reference = leaked.reference();
        Path path = leaked.path();

        LeakReport gcReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP
                && report.className().equals(BrokenTrackedTempFile.class.getName()),
            Duration.ofSeconds(10)
        );
        LeakReport failureReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.FALLBACK_CLEANUP_FAILED
                && report.className().equals(BrokenTrackedTempFile.class.getName()),
            Duration.ofSeconds(10)
        );

        assertEquals(BrokenTrackedTempFile.class.getName(), gcReport.className());
        assertEquals(BrokenTrackedTempFile.class.getName(), failureReport.className());
        assertTrue(Files.exists(path));
        assertTrue(failureReport.message().contains(ExplodingPathFallbackCleanup.class.getName()));
        assertTrue(failureReport.message().contains("simulated fallback failure"));
        assertEquals(ExplodingPathFallbackCleanup.class.getName(), failureReport.fallbackActionClassName());
        assertEquals(java.io.IOException.class.getName(), failureReport.failureClassName());
        assertTrue(failureReport.failureMessage().contains("simulated fallback failure"));

        List<LeakReport> reports = reporter.snapshot();
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP));
        assertTrue(reports.stream().anyMatch(report -> report.type() == LeakReportType.FALLBACK_CLEANUP_FAILED));

        Files.deleteIfExists(path);
    }

    private LeakedBrokenTrackedTempFile createLeakedBrokenTrackedTempFile(String prefix) throws Exception {
        BrokenTrackedTempFile leaked = new BrokenTrackedTempFile(prefix);
        return new LeakedBrokenTrackedTempFile(new WeakReference<>(leaked), leaked.path());
    }

    private static String extractPathSuffix(LeakReport report) {
        String failureMessage = report.failureMessage();
        if (failureMessage == null) {
            return "";
        }
        int index = failureMessage.indexOf("simulated fallback failure for ");
        if (index < 0) {
            return "";
        }
        return failureMessage.substring(index + "simulated fallback failure for ".length()).trim();
    }

    private record LeakedBrokenTrackedTempFile(WeakReference<BrokenTrackedTempFile> reference, Path path) {
    }
}
