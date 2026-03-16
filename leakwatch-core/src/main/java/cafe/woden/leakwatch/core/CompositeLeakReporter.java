package cafe.woden.leakwatch.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * {@link LeakReporter} that forwards each report to several delegates.
 */
public final class CompositeLeakReporter implements LeakReporter {
    private final List<LeakReporter> delegates;

    public CompositeLeakReporter(LeakReporter... delegates) {
        this(Arrays.asList(delegates));
    }

    public CompositeLeakReporter(List<LeakReporter> delegates) {
        this.delegates = List.copyOf(delegates);
        if (this.delegates.isEmpty()) {
            throw new IllegalArgumentException("At least one LeakReporter is required.");
        }
        this.delegates.forEach(delegate -> Objects.requireNonNull(delegate, "delegate"));
    }

    @Override
    public void report(LeakReport report) {
        for (LeakReporter delegate : delegates) {
            delegate.report(report);
        }
    }
}
