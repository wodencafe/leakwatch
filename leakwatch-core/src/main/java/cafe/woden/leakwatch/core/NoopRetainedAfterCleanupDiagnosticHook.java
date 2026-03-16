package cafe.woden.leakwatch.core;

/**
 * Public singleton no-op implementation of {@link RetainedAfterCleanupDiagnosticHook}.
 * <p>
 * Most callers can simply rely on {@link RetainedAfterCleanupDiagnosticHook#noop()} or
 * {@link LeakWatchConfig#defaults()}, but exposing the singleton explicitly keeps the SPI easy to reference in tests
 * and framework glue.
 */
public final class NoopRetainedAfterCleanupDiagnosticHook implements RetainedAfterCleanupDiagnosticHook {
    public static final NoopRetainedAfterCleanupDiagnosticHook INSTANCE = new NoopRetainedAfterCleanupDiagnosticHook();

    private NoopRetainedAfterCleanupDiagnosticHook() {
    }

    @Override
    public void onRetainedAfterCleanup(RetainedAfterCleanupDiagnosticContext context) {
        // Intentionally empty.
    }
}
