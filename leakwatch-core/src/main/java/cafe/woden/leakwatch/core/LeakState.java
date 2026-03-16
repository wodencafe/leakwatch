package cafe.woden.leakwatch.core;

/**
 * Internal lifecycle state for a tracked instance.
 */
public enum LeakState {
    TRACKED,
    CLEANED,
    RETAINED_AFTER_CLEANUP,
    GC_WITHOUT_CLEANUP
}
