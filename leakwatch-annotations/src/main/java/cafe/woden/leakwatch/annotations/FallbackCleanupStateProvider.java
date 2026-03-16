package cafe.woden.leakwatch.annotations;

/**
 * Exposes detached state that can be used by a {@link FallbackCleanupAction}.
 *
 * @param <T> detached state type
 */
public interface FallbackCleanupStateProvider<T> {
    /**
     * Returns the state needed for fallback cleanup.
     *
     * <p>The returned value should not depend on the original object staying alive.
     *
     * @return detached cleanup state
     */
    T leakWatchFallbackCleanupState();
}
