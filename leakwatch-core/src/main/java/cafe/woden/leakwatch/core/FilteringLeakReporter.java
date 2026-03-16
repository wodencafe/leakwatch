package cafe.woden.leakwatch.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * {@link LeakReporter} wrapper that drops reports that do not match a predicate.
 */
public final class FilteringLeakReporter implements LeakReporter {
    private final LeakReporter delegate;
    private final Predicate<LeakReport> filter;
    private final AtomicLong rejectedReports = new AtomicLong();

    public FilteringLeakReporter(LeakReporter delegate, Predicate<LeakReport> filter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    /**
     * Creates a reporter that forwards every report except the supplied types.
     */
    public static FilteringLeakReporter excludingTypes(LeakReporter delegate, Set<LeakReportType> excludedTypes) {
        Set<LeakReportType> excluded = Set.copyOf(Objects.requireNonNull(excludedTypes, "excludedTypes"));
        return new FilteringLeakReporter(delegate, report -> !excluded.contains(report.type()));
    }

    /**
     * Creates a reporter that forwards only the supplied report types.
     */
    public static FilteringLeakReporter includingOnlyTypes(LeakReporter delegate, Set<LeakReportType> allowedTypes) {
        Set<LeakReportType> allowed = Set.copyOf(Objects.requireNonNull(allowedTypes, "allowedTypes"));
        return new FilteringLeakReporter(delegate, report -> allowed.contains(report.type()));
    }

    @Override
    public void report(LeakReport report) {
        Objects.requireNonNull(report, "report");
        if (filter.test(report)) {
            delegate.report(report);
        } else {
            rejectedReports.incrementAndGet();
        }
    }

    /**
     * Returns how many reports were filtered out.
     */
    public long rejectedReports() {
        return rejectedReports.get();
    }
}
