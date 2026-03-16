package cafe.woden.leakwatch.core;

/**
 * Optional hook that runs when LeakWatch emits a {@link LeakReportType#RETAINED_AFTER_CLEANUP} report.
 * <p>
 * This hook is intended for diagnostic escalation rather than normal reporting. Typical implementations might
 * trigger a heap dump, start a focused JFR recording, enqueue a follow-up workflow, or attach additional notes to a
 * surrounding observability system.
 * <p>
 * Implementations should be lightweight, defensive, and non-blocking where possible. LeakWatch already emits the
 * human-readable report through its normal {@link LeakReporter}; this hook exists for optional extra investigation.
 */
@FunctionalInterface
public interface RetainedAfterCleanupDiagnosticHook {
    /**
     * Called after LeakWatch emits a retained-after-cleanup report.
     *
     * @param context structured diagnostic context for the retained-after-cleanup signal
     */
    void onRetainedAfterCleanup(RetainedAfterCleanupDiagnosticContext context);

    /**
     * Returns the default no-op diagnostic hook.
     *
     * @return singleton no-op hook
     */
    static RetainedAfterCleanupDiagnosticHook noop() {
        return NoopRetainedAfterCleanupDiagnosticHook.INSTANCE;
    }
}
