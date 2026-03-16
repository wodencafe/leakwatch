package cafe.woden.leakwatch.sample.retentioninstrumentation;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;

public final class InstrumentationRetentionProbeMain {
    private InstrumentationRetentionProbeMain() {
    }

    public static void main(String[] args) {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(LeakWatchConfig.defaults(), reporter);

        String description = ShallowSizeEstimators.autoDiscover().description();
        new InstrumentationRetentionTrackedObject();
        new InstrumentationRetentionTrackedObject();
        new InstrumentationRetentionTrackedObject();

        LeakReport report = reporter.snapshot().stream()
            .filter(candidate -> candidate.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED)
            .findFirst()
            .orElse(null);

        System.out.println("DISCOVERED_ESTIMATOR=" + description);
        System.out.println("HAS_APPROX_BYTES_REPORT=" + (report != null));
        if (report != null) {
            System.out.println("REPORT_CLASS=" + report.className());
            System.out.println("REPORT_APPROX_BYTES=" + report.retentionApproxShallowBytes());
            System.out.println("REPORT_MAX_APPROX_BYTES=" + report.retentionMaxApproxShallowBytes());
        }
    }
}
