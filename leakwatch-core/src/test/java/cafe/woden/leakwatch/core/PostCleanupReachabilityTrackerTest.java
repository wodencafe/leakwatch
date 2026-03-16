package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.ExpectUnreachableAfterCleanup;
import cafe.woden.leakwatch.annotations.LeakTracked;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PostCleanupReachabilityTrackerTest {
    private static final Duration REPORT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration GC_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void reportsObjectsThatRemainReachableAfterCleanup() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        RetainedResource.clearRetained();
        try (LeakRegistry registry = new LeakRegistry(testConfig(), reporter)) {
            RetainedResource retained = new RetainedResource();
            long id = registry.trackLifecycle(retained, RetainedResource.class);
            assertNotEquals(-1L, id);
            registry.markCleaned(retained, "close");
            retained.retain();
            retained = null;

            GcAwaiter.awaitCondition(
                REPORT_TIMEOUT,
                () -> reporter.snapshot().stream().anyMatch(report -> report.type() == LeakReportType.RETAINED_AFTER_CLEANUP),
                () -> "Expected retained-after-cleanup report. Reports seen: " + reporter.snapshot()
            );

            LeakReport report = reporter.snapshot().stream()
                .filter(candidate -> candidate.type() == LeakReportType.RETAINED_AFTER_CLEANUP)
                .findFirst()
                .orElseThrow();

            assertEquals(RetainedResource.class.getName(), report.className());
            assertEquals(Long.valueOf(75L), report.postCleanupGraceMillis());
            assertNotNull(report.allocationSite());
            assertNotNull(report.cleanupSite());
            assertNotNull(report.cleanedAt());
            assertNotNull(report.ageSinceCleanup());
            assertTrue(report.ageSinceCleanup().toMillis() >= 0L);
            assertTrue(report.tags().contains("tracked"));
            assertTrue(report.tags().contains("post-cleanup"));
        } finally {
            RetainedResource.clearRetained();
        }
    }


    @Test
    void invokesDiagnosticHookAfterRetainedAfterCleanupReport() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        AtomicReference<RetainedAfterCleanupDiagnosticContext> captured = new AtomicReference<>();
        RetainedResource.clearRetained();
        try (LeakRegistry registry = new LeakRegistry(testConfig().withRetainedAfterCleanupDiagnosticHook(captured::set), reporter)) {
            RetainedResource retained = new RetainedResource();
            long id = registry.trackLifecycle(retained, RetainedResource.class);
            assertNotEquals(-1L, id);
            registry.markCleaned(retained, "close");
            retained.retain();
            retained = null;

            GcAwaiter.awaitCondition(
                REPORT_TIMEOUT,
                () -> captured.get() != null,
                () -> "Expected retained-after-cleanup diagnostic hook to fire"
            );

            RetainedAfterCleanupDiagnosticContext context = captured.get();
            assertNotNull(context);
            assertEquals(RetainedResource.class.getName(), context.className());
            assertEquals("close", context.observedCleanupMethodName());
            assertTrue(context.tags().contains("tracked"));
            assertTrue(context.tags().contains("post-cleanup"));
            assertEquals(Long.valueOf(75L), context.postCleanupGraceMillis());
            assertNotNull(context.allocationSite());
            assertNotNull(context.cleanupSite());
            assertEquals(LeakReportType.RETAINED_AFTER_CLEANUP, context.report().type());
        } finally {
            RetainedResource.clearRetained();
        }
    }

    @Test
    void doesNotReportWhenObjectBecomesUnreachableBeforeGraceDeadline() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        try (LeakRegistry registry = new LeakRegistry(testConfig(), reporter)) {
            EphemeralResource ephemeral = new EphemeralResource();
            long id = registry.trackLifecycle(ephemeral, EphemeralResource.class);
            assertNotEquals(-1L, id);
            registry.markCleaned(ephemeral, "close");
            WeakReference<EphemeralResource> reference = new WeakReference<>(ephemeral);
            ephemeral = null;

            GcAwaiter.awaitCondition(
                GC_TIMEOUT,
                () -> reference.get() == null,
                () -> "Expected cleaned ephemeral resource to become unreachable"
            );
            Thread.sleep(700L);

            assertFalse(reporter.snapshot().stream().anyMatch(report -> report.type() == LeakReportType.RETAINED_AFTER_CLEANUP));
        }
    }

    @Test
    void expectUnreachableAfterCleanupDoesNotImplyLeakTracked() {
        LeakTracked leakTracked = TrackingAnnotationSupport.resolveLeakTracked(MetaOnlyResource.class);
        assertNull(leakTracked);
        assertNotNull(TrackingAnnotationSupport.resolveExpectUnreachableAfterCleanup(MetaOnlyResource.class));
    }

    private static LeakWatchConfig testConfig() {
        return new LeakWatchConfig(
            true,
            false,
            false,
            true,
            Set.of()
        );
    }

    @LeakTracked(captureStackTrace = true, tags = {"tracked"})
    @ExpectUnreachableAfterCleanup(gracePeriodMillis = 75L, captureCleanupStackTrace = true, tags = {"post-cleanup"})
    private static final class RetainedResource {
        private static final List<Object> RETAINED = new ArrayList<>();

        @CleanupMethod
        void close() {
        }

        void retain() {
            synchronized (RETAINED) {
                RETAINED.add(this);
            }
        }

        static void clearRetained() {
            synchronized (RETAINED) {
                RETAINED.clear();
            }
        }
    }

    @ExpectUnreachableAfterCleanup(gracePeriodMillis = 100L)
    private static final class MetaOnlyResource {
        @CleanupMethod
        void close() {
        }
    }

    @LeakTracked(captureStackTrace = true)
    @ExpectUnreachableAfterCleanup(gracePeriodMillis = 500L, captureCleanupStackTrace = true)
    private static final class EphemeralResource {
        @CleanupMethod
        void close() {
        }
    }
}
