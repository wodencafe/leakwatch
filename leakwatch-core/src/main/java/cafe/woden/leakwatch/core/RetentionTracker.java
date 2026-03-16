package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.RetentionSuspect;
import cafe.woden.leakwatch.annotations.Severity;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks weak live counts and optional shallow-size budgets for {@code @RetentionSuspect} types.
 */
public final class RetentionTracker {
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final ConcurrentMap<String, RetentionBucket> buckets = new ConcurrentHashMap<>();
    private final LeakReporter reporter;
    private final LeakWatchConfig config;
    private final StartupDiagnostics diagnostics;
    private final ShallowSizeEstimator shallowSizeEstimator;

    public RetentionTracker(LeakReporter reporter) {
        this(reporter, LeakWatchConfig.defaults(), new StartupDiagnostics(), ShallowSizeEstimator.unsupported());
    }

    public RetentionTracker(
        LeakReporter reporter,
        LeakWatchConfig config,
        StartupDiagnostics diagnostics,
        ShallowSizeEstimator shallowSizeEstimator
    ) {
        this.reporter = reporter;
        this.config = config;
        this.diagnostics = diagnostics;
        this.shallowSizeEstimator = shallowSizeEstimator;
    }

    public void track(Object instance, Class<?> type, RetentionSuspect annotation) {
        drainQueue();

        String className = type.getName();
        RetentionBucket bucket = buckets.computeIfAbsent(className, ignored -> new RetentionBucket());
        bucket.maxLiveInstances = annotation.maxLiveInstances();
        bucket.maxApproxShallowBytes = annotation.maxApproxShallowBytes();
        bucket.tags = Arrays.stream(annotation.tags()).collect(
            LinkedHashSet::new,
            LinkedHashSet::add,
            LinkedHashSet::addAll
        );
        bucket.severity = annotation.severity();

        Throwable allocationSite = annotation.captureStackTrace()
            ? new Exception("LeakWatch retention allocation site")
            : null;

        long approxShallowBytes = estimateApproxShallowBytes(instance, className, annotation);
        TrackedWeakReference ref = new TrackedWeakReference(instance, queue, className, allocationSite, Instant.now(), approxShallowBytes);
        bucket.references.put(ref, Boolean.TRUE);
        bucket.approxShallowBytes.addAndGet(approxShallowBytes);

        long liveCount = bucket.liveCount();
        long maxLiveInstances = bucket.maxLiveInstances;
        long totalApproxShallowBytes = bucket.approxShallowBytes();
        long maxApproxShallowBytes = bucket.maxApproxShallowBytes;

        if (maxLiveInstances > -1 && liveCount > maxLiveInstances && bucket.overCountBudgetReported.compareAndSet(false, true)) {
            reporter.report(new LeakReport(
                LeakReportType.RETENTION_COUNT_EXCEEDED,
                -1L,
                className,
                Set.of(),
                null,
                ref.createdAt,
                Duration.ZERO,
                Set.copyOf(bucket.tags),
                allocationSite,
                "Live instance count exceeded configured maximum. liveCount=" + liveCount + ", maxLiveInstances=" + maxLiveInstances,
                bucket.severity,
                liveCount,
                maxLiveInstances,
                totalApproxShallowBytes,
                maxApproxShallowBytes > -1 ? maxApproxShallowBytes : null,
                null
            ));
        }

        if (maxApproxShallowBytes > -1
            && shallowSizeEstimator.isAvailable()
            && totalApproxShallowBytes > maxApproxShallowBytes
            && bucket.overApproxBytesBudgetReported.compareAndSet(false, true)) {
            reporter.report(new LeakReport(
                LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED,
                -1L,
                className,
                Set.of(),
                null,
                ref.createdAt,
                Duration.ZERO,
                Set.copyOf(bucket.tags),
                allocationSite,
                "Approximate shallow-byte budget exceeded. approxShallowBytes=" + totalApproxShallowBytes
                    + ", maxApproxShallowBytes=" + maxApproxShallowBytes
                    + ", overBudget=" + (totalApproxShallowBytes - maxApproxShallowBytes)
                    + ", liveCount=" + liveCount,
                bucket.severity,
                liveCount,
                maxLiveInstances > -1 ? maxLiveInstances : null,
                totalApproxShallowBytes,
                maxApproxShallowBytes,
                totalApproxShallowBytes - maxApproxShallowBytes
            ));
        }
    }

    public long liveCount(String className) {
        drainQueue();
        RetentionBucket bucket = buckets.get(className);
        return bucket == null ? 0L : bucket.liveCount();
    }

    public long approxShallowBytes(String className) {
        drainQueue();
        RetentionBucket bucket = buckets.get(className);
        return bucket == null ? 0L : bucket.approxShallowBytes();
    }

    public void drainQueue() {
        TrackedWeakReference ref;
        while ((ref = (TrackedWeakReference) queue.poll()) != null) {
            RetentionBucket bucket = buckets.get(ref.className);
            if (bucket != null) {
                bucket.references.remove(ref);
                bucket.approxShallowBytes.addAndGet(-ref.approxShallowBytes);
                if (bucket.maxLiveInstances > -1 && bucket.liveCount() <= bucket.maxLiveInstances) {
                    bucket.overCountBudgetReported.set(false);
                }
                if (bucket.maxApproxShallowBytes > -1 && bucket.approxShallowBytes() <= bucket.maxApproxShallowBytes) {
                    bucket.overApproxBytesBudgetReported.set(false);
                }
            }
        }
    }

    private long estimateApproxShallowBytes(Object instance, String className, RetentionSuspect annotation) {
        if (annotation.maxApproxShallowBytes() < 0) {
            return 0L;
        }

        if (!shallowSizeEstimator.isAvailable()) {
            diagnostics.warnRetentionSizingUnavailableOnce(className, config, reporter, shallowSizeEstimator.description());
            return 0L;
        }

        try {
            return Math.max(0L, shallowSizeEstimator.estimateShallowSize(instance));
        } catch (RuntimeException ex) {
            diagnostics.warnRetentionSizingFailureOnce(className, config, reporter, shallowSizeEstimator.description(), ex);
            return 0L;
        }
    }

    private static final class RetentionBucket {
        private final ConcurrentMap<TrackedWeakReference, Boolean> references = new ConcurrentHashMap<>();
        private final AtomicLong approxShallowBytes = new AtomicLong();
        private final AtomicBoolean overCountBudgetReported = new AtomicBoolean(false);
        private final AtomicBoolean overApproxBytesBudgetReported = new AtomicBoolean(false);
        private volatile long maxLiveInstances = -1L;
        private volatile long maxApproxShallowBytes = -1L;
        private volatile Severity severity = Severity.WARN;
        private volatile Set<String> tags = Set.of();

        private long liveCount() {
            return references.size();
        }

        private long approxShallowBytes() {
            return Math.max(0L, approxShallowBytes.get());
        }
    }

    private static final class TrackedWeakReference extends WeakReference<Object> {
        private final String className;
        private final Instant createdAt;
        private final long approxShallowBytes;

        private TrackedWeakReference(
            Object referent,
            ReferenceQueue<Object> queue,
            String className,
            Throwable allocationSite,
            Instant createdAt,
            long approxShallowBytes
        ) {
            super(referent, queue);
            this.className = className;
            this.createdAt = createdAt;
            this.approxShallowBytes = approxShallowBytes;
        }
    }
}
