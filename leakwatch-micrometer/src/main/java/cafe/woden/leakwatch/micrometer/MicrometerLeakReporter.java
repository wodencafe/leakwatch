package cafe.woden.leakwatch.micrometer;

import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReporter;
import cafe.woden.leakwatch.core.LeakReportType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

/**
 * {@link cafe.woden.leakwatch.core.LeakReporter} that records basic LeakWatch counters in Micrometer.
 */
public final class MicrometerLeakReporter implements LeakReporter {
    public static final String TOTAL_REPORTS_METER = "leakwatch.reports.total";
    public static final String REPORTS_BY_TYPE_METER = "leakwatch.reports.by.type";
    public static final String REPORTS_BY_CLASS_METER = "leakwatch.reports.by.class";
    public static final String FALLBACK_FAILURES_METER = "leakwatch.fallback.cleanup.failures";

    private final MeterRegistry registry;

    public MicrometerLeakReporter(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void report(LeakReport report) {
        Objects.requireNonNull(report, "report");

        Counter.builder(TOTAL_REPORTS_METER)
            .description("Total LeakWatch reports observed")
            .register(registry)
            .increment();

        Counter.builder(REPORTS_BY_TYPE_METER)
            .description("LeakWatch reports grouped by report type")
            .tag("report_type", report.type().name())
            .register(registry)
            .increment();

        Counter.builder(REPORTS_BY_CLASS_METER)
            .description("LeakWatch reports grouped by tracked class and report type")
            .tag("class_name", safeValue(report.className()))
            .tag("report_type", report.type().name())
            .register(registry)
            .increment();

        if (report.type() == LeakReportType.FALLBACK_CLEANUP_FAILED) {
            Counter.builder(FALLBACK_FAILURES_METER)
                .description("LeakWatch detached fallback cleanup failures")
                .tag("class_name", safeValue(report.className()))
                .tag("fallback_action", safeValue(report.fallbackActionClassName()))
                .tag("failure_class", safeValue(report.failureClassName()))
                .register(registry)
                .increment();
        }
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
