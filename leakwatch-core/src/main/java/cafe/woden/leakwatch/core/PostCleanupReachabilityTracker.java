package cafe.woden.leakwatch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class PostCleanupReachabilityTracker implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PostCleanupReachabilityTracker.class);

    private final LeakReporter reporter;
    private final RetainedAfterCleanupDiagnosticHook diagnosticHook;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<Long, PostCleanupWatch> watches = new ConcurrentHashMap<>();

    PostCleanupReachabilityTracker(LeakReporter reporter, RetainedAfterCleanupDiagnosticHook diagnosticHook) {
        this.reporter = reporter;
        this.diagnosticHook = diagnosticHook;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new WatcherThreadFactory());
    }

    void watch(
        Object instance,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Instant cleanedAt,
        Set<String> tags,
        Throwable allocationSite,
        Throwable cleanupSite,
        long gracePeriodMillis
    ) {
        long delayMillis = Math.max(0L, gracePeriodMillis);
        PostCleanupWatch watch = new PostCleanupWatch(
            id,
            className,
            Set.copyOf(expectedCleanupMethods),
            observedCleanupMethodName,
            createdAt,
            cleanedAt,
            Set.copyOf(tags),
            allocationSite,
            cleanupSite,
            delayMillis,
            new WeakReference<>(instance)
        );
        watches.put(id, watch);
        scheduler.schedule(() -> check(id), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void check(long id) {
        PostCleanupWatch watch = watches.remove(id);
        if (watch == null) {
            return;
        }
        if (watch.reference().get() == null) {
            return;
        }

        Instant now = Instant.now();
        LeakReport report = new LeakReport(
            LeakReportType.RETAINED_AFTER_CLEANUP,
            watch.id(),
            watch.className(),
            watch.expectedCleanupMethods(),
            watch.observedCleanupMethodName(),
            watch.createdAt(),
            Duration.between(watch.createdAt(), now),
            watch.tags(),
            watch.allocationSite(),
            "Object remained strongly reachable after explicit cleanup beyond the configured grace period. gracePeriodMillis="
                + watch.gracePeriodMillis()
                + ", ageSinceCleanupMillis=" + Duration.between(watch.cleanedAt(), now).toMillis(),
            null,
            watch.cleanedAt(),
            Duration.between(watch.cleanedAt(), now),
            watch.cleanupSite(),
            watch.gracePeriodMillis()
        );
        reporter.report(report);
        invokeDiagnosticHook(report);
    }

    private void invokeDiagnosticHook(LeakReport report) {
        try {
            diagnosticHook.onRetainedAfterCleanup(RetainedAfterCleanupDiagnosticContext.from(report));
        } catch (Exception exception) {
            log.warn(
                "LeakWatch retained-after-cleanup diagnostic hook failed for class={} id={}: {}",
                report.className(),
                report.id(),
                exception.toString(),
                exception
            );
        }
    }

    @Override
    public void close() {
        watches.clear();
        scheduler.shutdownNow();
    }

    private record PostCleanupWatch(
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Instant cleanedAt,
        Set<String> tags,
        Throwable allocationSite,
        Throwable cleanupSite,
        long gracePeriodMillis,
        WeakReference<Object> reference
    ) {
    }

    private static final class WatcherThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "leakwatch-post-cleanup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
