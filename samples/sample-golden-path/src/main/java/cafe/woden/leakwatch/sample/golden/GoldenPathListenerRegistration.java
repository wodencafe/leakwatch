package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.ExpectUnreachableAfterCleanup;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Intentionally buggy sample type that stays registered after cleanup so the golden-path sample can
 * demonstrate post-cleanup reachability reporting.
 */
@LeakTracked(captureStackTrace = true, tags = {"sample", "golden-path", "listener"})
@ExpectUnreachableAfterCleanup(
    gracePeriodMillis = 150L,
    captureCleanupStackTrace = true,
    tags = {"post-cleanup"}
)
public final class GoldenPathListenerRegistration implements AutoCloseable {
    private static final List<GoldenPathListenerRegistration> REGISTRY = new CopyOnWriteArrayList<>();

    private final String listenerId;
    private volatile boolean closed;

    public GoldenPathListenerRegistration(String listenerId) {
        this.listenerId = listenerId;
        REGISTRY.add(this);
    }

    public String listenerId() {
        return listenerId;
    }

    public boolean closed() {
        return closed;
    }

    public static int retainedCount() {
        return REGISTRY.size();
    }

    public static void clearRetained() {
        REGISTRY.clear();
    }

    @Override
    @CleanupMethod
    public void close() {
        closed = true;
        // Intentionally wrong for the sample: this should remove the listener from REGISTRY.
    }
}
