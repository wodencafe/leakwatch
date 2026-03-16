package cafe.woden.leakwatch.annotations;

/**
 * Performs fallback cleanup using state captured from the original object.
 *
 * @param <T> detached state type passed to the cleanup action
 */
@FunctionalInterface
public interface FallbackCleanupAction<T> {
    /**
     * Runs fallback cleanup.
     *
     * @param detachedState state captured from the original object
     * @throws Exception if cleanup fails
     */
    void cleanup(T detachedState) throws Exception;
}
