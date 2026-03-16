package cafe.woden.leakwatch.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@link LeakReporter} that stores reports in memory.
 * <p>
 * Mostly useful for tests, demos, or custom assertions.
 */
public final class InMemoryLeakReporter implements LeakReporter {
    private final List<LeakReport> reports = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void report(LeakReport report) {
        reports.add(report);
    }

    /**
     * Returns an immutable snapshot of all reports captured so far.
     */
    public List<LeakReport> snapshot() {
        synchronized (reports) {
            return List.copyOf(reports);
        }
    }

    /**
     * Removes any reports captured so far.
     */
    public void clear() {
        reports.clear();
    }
}
