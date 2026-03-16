package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.ExpectUnreachableAfterCleanup;
import cafe.woden.leakwatch.annotations.LeakTracked;
import cafe.woden.leakwatch.annotations.RetentionSuspect;

import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central runtime registry that tracks lifecycle and retention state for annotated objects.
 * <p>
 * In most applications this is accessed through {@link LeakWatchRuntime#registry()}.
 */
public final class LeakRegistry implements AutoCloseable {
    private final Cleaner cleaner = Cleaner.create();
    private final AtomicLong ids = new AtomicLong();
    private final ConcurrentMap<Long, LeakHandle> active = new ConcurrentHashMap<>();
    private final Map<Object, LeakHandle> byInstance = Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private final ConcurrentMap<Class<?>, CleanupMetadata> cleanupMetadataCache = new ConcurrentHashMap<>();

    private final LeakWatchConfig config;
    private final LeakReporter reporter;
    private final RetentionTracker retentionTracker;
    private final StartupDiagnostics diagnostics;
    private final PostCleanupReachabilityTracker postCleanupTracker;

    public LeakRegistry(LeakWatchConfig config, LeakReporter reporter) {
        this(config, reporter, ShallowSizeEstimator.unsupported());
    }

    public LeakRegistry(LeakWatchConfig config, LeakReporter reporter, ShallowSizeEstimator shallowSizeEstimator) {
        this.config = config;
        this.reporter = reporter;
        this.diagnostics = new StartupDiagnostics();
        this.retentionTracker = new RetentionTracker(reporter, config, diagnostics, shallowSizeEstimator);
        this.postCleanupTracker = new PostCleanupReachabilityTracker(reporter, config.retainedAfterCleanupDiagnosticHook());
    }

    /**
     * Starts lifecycle tracking for an instance using the resolved {@code @LeakTracked} annotation.
     *
     * @return handle id, or {@code -1} when the instance was not tracked
     */
    public long trackLifecycle(Object instance, Class<?> type) {
        return trackLifecycle(instance, type, TrackingAnnotationSupport.resolveLeakTracked(type));
    }

    /**
     * Starts lifecycle tracking for an instance using an explicit {@code @LeakTracked} annotation value.
     *
     * @return handle id, or {@code -1} when the instance was not tracked
     */
    public long trackLifecycle(Object instance, Class<?> type, LeakTracked annotation) {
        diagnostics.logStartupOnce(config);

        if (!config.enabled()) {
            return -1L;
        }

        String className = type.getName();
        if (config.isExcluded(className)) {
            return -1L;
        }

        if (annotation == null) {
            return -1L;
        }

        synchronized (byInstance) {
            LeakHandle existing = byInstance.get(instance);
            if (existing != null) {
                return existing.id();
            }
        }

        diagnostics.noteLeakTrackedClass(className);

        CleanupMetadata metadata = cleanupMetadataCache.computeIfAbsent(type, ClassIntrospector::inspect);
        Set<String> expectedCleanupMethods = expectedCleanupMethods(metadata, annotation);
        if (expectedCleanupMethods.isEmpty()) {
            maybeWarnStrictLifecycleConfiguration(type, annotation, metadata);
            return -1L;
        }

        long id = ids.incrementAndGet();
        Throwable allocationSite = annotation.captureStackTrace() || config.defaultCaptureStackTraces()
            ? new Exception("LeakWatch allocation site")
            : null;
        FallbackCleanupPlan fallbackCleanupPlan = FallbackCleanupSupport.resolve(instance, type, config, reporter, diagnostics);
        ExpectUnreachableAfterCleanup expect = TrackingAnnotationSupport.resolveExpectUnreachableAfterCleanup(type);
        PostCleanupExpectation postCleanupExpectation = expect == null
            ? null
            : new PostCleanupExpectation(expect.gracePeriodMillis(), expect.captureCleanupStackTrace());

        LeakHandle handle = new LeakHandle(
            id,
            type,
            allocationSite,
            mergeTags(annotation.tags(), expect == null ? null : expect.tags()),
            expectedCleanupMethods,
            fallbackCleanupPlan,
            postCleanupExpectation
        );
        active.put(id, handle);
        synchronized (byInstance) {
            byInstance.put(instance, handle);
        }

        Cleaner.Cleanable cleanable = cleaner.register(instance, () -> handle.onCleanerRun(reporter, active));
        handle.attach(cleanable);

        return id;
    }

    /**
     * Records a retention-suspect instance for weak live-count and optional byte-budget monitoring.
     */
    public void trackRetention(Object instance, Class<?> type, RetentionSuspect annotation) {
        diagnostics.logStartupOnce(config);

        if (!config.enabled()) {
            return;
        }

        String className = type.getName();
        if (config.isExcluded(className)) {
            return;
        }

        diagnostics.noteRetentionClass(className);
        retentionTracker.track(instance, type, annotation);
    }

    /**
     * Marks an instance as cleaned after one of its recognized cleanup methods runs.
     */
    public void markCleaned(Object instance, String cleanupMethodName) {
        LeakHandle handle;
        synchronized (byInstance) {
            handle = byInstance.remove(instance);
        }
        if (handle != null) {
            handle.markCleaned(cleanupMethodName, instance, postCleanupTracker);
        }
    }

    /**
     * Returns {@code true} when the method name is one of the recognized conventional cleanup methods for the type.
     */
    public boolean isConventionCleanupMethod(Class<?> type, String methodName) {
        CleanupMetadata metadata = cleanupMetadataCache.computeIfAbsent(type, ClassIntrospector::inspect);
        return metadata.hasConventionalMethod(methodName);
    }

    /**
     * Returns the number of lifecycle-tracked instances that are currently still active.
     */
    public int activeCount() {
        return active.size();
    }

    /**
     * Returns the current weak live count for the given tracked class name.
     */
    public long retentionLiveCount(String className) {
        return retentionTracker.liveCount(className);
    }

    /**
     * Returns the current approximate summed shallow bytes for the given tracked class name.
     */
    public long retentionApproxShallowBytes(String className) {
        return retentionTracker.approxShallowBytes(className);
    }

    @Override
    public void close() {
        postCleanupTracker.close();
    }

    private void maybeWarnStrictLifecycleConfiguration(Class<?> type, LeakTracked annotation, CleanupMetadata metadata) {
        if (!config.strictMode()) {
            return;
        }

        boolean conventionEnabled = annotation.conventionCleanupDetection() && config.defaultConventionCleanupDetection();
        boolean hasRecognizedCleanup = metadata.hasExplicitCleanupMethod() || (conventionEnabled && !metadata.conventionalCleanupMethods().isEmpty());

        if (!hasRecognizedCleanup) {
            diagnostics.warnStrictOnce(
                type.getName(),
                type.getName(),
                reporter,
                "@LeakTracked class " + type.getName() + " was not lifecycle-tracked because it has no @CleanupMethod and no recognized conventional cleanup method. Use @RetentionSuspect for heuristic retention monitoring."
            );
        }
    }

    private Set<String> expectedCleanupMethods(CleanupMetadata metadata, LeakTracked annotation) {
        LinkedHashSet<String> methods = new LinkedHashSet<>(metadata.explicitCleanupMethods());
        boolean conventionEnabled = annotation.conventionCleanupDetection() && config.defaultConventionCleanupDetection();
        if (conventionEnabled) {
            methods.addAll(metadata.conventionalCleanupMethods());
        }
        return Set.copyOf(methods);
    }

    private String[] mergeTags(String[] lifecycleTags, String[] postCleanupTags) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (lifecycleTags != null) {
            Collections.addAll(merged, lifecycleTags);
        }
        if (postCleanupTags != null) {
            Collections.addAll(merged, postCleanupTags);
        }
        return merged.toArray(String[]::new);
    }
}
