package cafe.woden.leakwatch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal state for one tracked instance.
 * <p>
 * Most applications will work with {@link LeakRegistry} and {@link LeakReport} instead of this type directly.
 */
public final class LeakHandle {
    private static final Logger log = LoggerFactory.getLogger(LeakHandle.class);

    private final long id;
    private final String className;
    private final Instant createdAt;
    private final Throwable allocationSite;
    private final Set<String> tags;
    private final Set<String> expectedCleanupMethods;
    private final FallbackCleanupPlan fallbackCleanupPlan;
    private final PostCleanupExpectation postCleanupExpectation;
    private final AtomicBoolean cleaned = new AtomicBoolean(false);

    private volatile Cleaner.Cleanable cleanable;
    private volatile String observedCleanupMethodName;
    private volatile LeakState state = LeakState.TRACKED;

    public LeakHandle(
        long id,
        Class<?> type,
        Throwable allocationSite,
        String[] tags,
        Set<String> expectedCleanupMethods,
        FallbackCleanupPlan fallbackCleanupPlan,
        PostCleanupExpectation postCleanupExpectation
    ) {
        this.id = id;
        this.className = type.getName();
        this.createdAt = Instant.now();
        this.allocationSite = allocationSite;
        this.tags = Arrays.stream(tags).collect(
            LinkedHashSet::new,
            LinkedHashSet::add,
            LinkedHashSet::addAll
        );
        this.expectedCleanupMethods = new LinkedHashSet<>(expectedCleanupMethods);
        this.fallbackCleanupPlan = fallbackCleanupPlan;
        this.postCleanupExpectation = postCleanupExpectation;
    }

    public void attach(Cleaner.Cleanable cleanable) {
        this.cleanable = cleanable;
    }

    public boolean markCleaned(String cleanupMethodName, Object instance, PostCleanupReachabilityTracker postCleanupTracker) {
        if (cleaned.compareAndSet(false, true)) {
            Instant cleanedAt = Instant.now();
            Throwable cleanupSite = postCleanupExpectation != null && postCleanupExpectation.captureCleanupStackTrace()
                ? new Exception("LeakWatch cleanup site")
                : null;
            this.observedCleanupMethodName = cleanupMethodName;
            this.state = LeakState.CLEANED;
            Cleaner.Cleanable current = this.cleanable;
            if (current != null) {
                current.clean();
            }
            if (postCleanupExpectation != null) {
                postCleanupTracker.watch(
                    instance,
                    id,
                    className,
                    Set.copyOf(expectedCleanupMethods),
                    cleanupMethodName,
                    createdAt,
                    cleanedAt,
                    Set.copyOf(tags),
                    allocationSite,
                    cleanupSite,
                    postCleanupExpectation.gracePeriodMillis()
                );
            }
            return true;
        }
        return false;
    }

    public void onCleanerRun(LeakReporter reporter, ConcurrentMap<Long, LeakHandle> active) {
        active.remove(id);

        if (cleaned.get()) {
            return;
        }

        this.state = LeakState.GC_WITHOUT_CLEANUP;

        reporter.report(new LeakReport(
            LeakReportType.GC_WITHOUT_CLEANUP,
            id,
            className,
            Set.copyOf(expectedCleanupMethods),
            observedCleanupMethodName,
            createdAt,
            Duration.between(createdAt, Instant.now()),
            Set.copyOf(tags),
            allocationSite,
            "Object was garbage collected before an explicit cleanup method was observed."
        ));

        runFallbackCleanupIfPresent(reporter);
    }

    private void runFallbackCleanupIfPresent(LeakReporter reporter) {
        if (fallbackCleanupPlan == null) {
            return;
        }

        try {
            fallbackCleanupPlan.execute();
            reporter.report(new LeakReport(
                LeakReportType.FALLBACK_CLEANUP_EXECUTED,
                id,
                className,
                Set.copyOf(expectedCleanupMethods),
                observedCleanupMethodName,
                createdAt,
                Duration.between(createdAt, Instant.now()),
                Set.copyOf(tags),
                allocationSite,
                "Detached fallback cleanup action executed after GC-before-cleanup. action=" + fallbackCleanupPlan.actionClassName(),
                fallbackCleanupPlan.actionClassName(),
                null,
                null
            ));
        } catch (Exception ex) {
            reporter.report(new LeakReport(
                LeakReportType.FALLBACK_CLEANUP_FAILED,
                id,
                className,
                Set.copyOf(expectedCleanupMethods),
                observedCleanupMethodName,
                createdAt,
                Duration.between(createdAt, Instant.now()),
                Set.copyOf(tags),
                allocationSite,
                "Detached fallback cleanup action failed after GC-before-cleanup. action="
                    + fallbackCleanupPlan.actionClassName()
                    + ", failure=" + ex.getClass().getName()
                    + (ex.getMessage() == null ? "" : ": " + ex.getMessage()),
                fallbackCleanupPlan.actionClassName(),
                ex.getClass().getName(),
                ex.getMessage()
            ));
            log.warn(
                "LeakWatch fallback cleanup action failed for class={} id={} action={}",
                className,
                id,
                fallbackCleanupPlan.actionClassName(),
                ex
            );
        }
    }

    public long id() {
        return id;
    }

    public LeakState state() {
        return state;
    }
}
