package cafe.woden.leakwatch.sample.dispose;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisposeIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @Test
    void disposeConventionRemovesTrackedInstance() {
        DisposableWindow window = new DisposableWindow("explicit-dispose");
        window.dispose();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void leakedDisposableReportsExpectedCleanupMethod() throws Exception {
        WeakReference<DisposableWindow> reference = createLeakedWindow();

        LeakReport leakReport = GcAwaiter.awaitReport(
            reference,
            reporter::snapshot,
            report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP,
            Duration.ofSeconds(10)
        );
        assertEquals(DisposableWindow.class.getName(), leakReport.className());
        assertEquals(Set.of("dispose"), leakReport.expectedCleanupMethods());
        assertNull(leakReport.observedCleanupMethodName());
    }

    private WeakReference<DisposableWindow> createLeakedWindow() {
        DisposableWindow leaked = new DisposableWindow("leaked-dispose-test");
        return new WeakReference<>(leaked);
    }
}
