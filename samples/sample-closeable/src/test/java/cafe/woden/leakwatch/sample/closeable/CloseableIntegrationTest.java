package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseableIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @Test
    void explicitCleanupRemovesTrackedInstance() {
        SampleResource resource = new SampleResource("explicit-cleanup");
        resource.close();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void conventionalCleanupRemovesTrackedInstance() {
        ConventionalCleanupResource resource = new ConventionalCleanupResource("conventional-cleanup");
        resource.shutdown();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void nonPublicAnnotatedCleanupStillRemovesTrackedInstance() {
        IndirectAnnotatedCleanupResource resource = new IndirectAnnotatedCleanupResource("indirect-cleanup");
        resource.release();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void leakedTrackedObjectReportsExpectedCleanupMethods() throws Exception {
        WeakReference<SampleResource> reference = createLeakedResource();

        LeakReport leakReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP,
            Duration.ofSeconds(10)
        );
        assertEquals(SampleResource.class.getName(), leakReport.className());
        assertEquals(Set.of("close"), leakReport.expectedCleanupMethods());
        assertNull(leakReport.observedCleanupMethodName());
    }

    @Test
    void misconfiguredLeakTrackedTypeIsNotLifecycleTracked() {
        new MisconfiguredTrackedResource("missing-cleanup");

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void strictModeReportsMisconfiguredLeakTrackedTypeOnlyOnce() {
        reporter = LeakWatchTestSupport.configureInMemory(true);

        new MisconfiguredTrackedResource("first");
        new MisconfiguredTrackedResource("second");

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.STRICT_MODE_WARNING)
            .toList();

        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains(MisconfiguredTrackedResource.class.getName()));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    @Test
    void disabledConventionDetectionRequiresExplicitCleanupMethod() {
        reporter = LeakWatchTestSupport.configureInMemory(true);

        ConventionDetectionDisabledResource resource = new ConventionDetectionDisabledResource("disabled-conventions");
        resource.shutdown();

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.STRICT_MODE_WARNING)
            .toList();

        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains(ConventionDetectionDisabledResource.class.getName()));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    private WeakReference<SampleResource> createLeakedResource() {
        SampleResource leaked = new SampleResource("leaked-test");
        return new WeakReference<>(leaked);
    }
}
