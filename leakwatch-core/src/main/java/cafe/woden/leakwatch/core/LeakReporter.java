package cafe.woden.leakwatch.core;

/**
 * Sink for {@link LeakReport} events.
 */
public interface LeakReporter {
    /**
     * Handles a single LeakWatch report.
     */
    void report(LeakReport report);
}
