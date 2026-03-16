package cafe.woden.leakwatch.sample.micrometer;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;
import cafe.woden.leakwatch.micrometer.MicrometerLeakReporter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public final class MeteredSampleApp {
    private MeteredSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            LeakWatchRuntime.configure(
                LeakWatchConfig.defaults(),
                new CompositeLeakReporter(
                    new Slf4jLeakReporter(),
                    new MicrometerLeakReporter(registry)
                )
            );

            MeteredSampleResource cleaned = new MeteredSampleResource("cleaned-metered");
            cleaned.close();

            new MeteredSampleResource("leaked-metered");

            System.gc();
            Thread.sleep(1500L);

            double totalReports = registry.get(MicrometerLeakReporter.TOTAL_REPORTS_METER).counter().count();
            double gcWithoutCleanupReports = registry.get(MicrometerLeakReporter.REPORTS_BY_TYPE_METER)
                .tag("report_type", LeakReportType.GC_WITHOUT_CLEANUP.name())
                .counter()
                .count();

            System.out.println("LeakWatch Micrometer totalReports=" + (long) totalReports);
            System.out.println("LeakWatch Micrometer gcWithoutCleanupReports=" + (long) gcWithoutCleanupReports);
        } finally {
            registry.close();
        }
    }
}
