package cafe.woden.leakwatch.sample.observability;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.LeakReportJsonFileReporter;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;
import cafe.woden.leakwatch.metrics.LeakMetricsSnapshot;
import cafe.woden.leakwatch.metrics.LeakReportMetrics;

import java.nio.file.Path;

public final class ObservabilitySampleApp {
    private ObservabilitySampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path outputFile = args.length > 0
            ? Path.of(args[0])
            : Path.of("build", "leakwatch", "reports.ndjson");

        LeakReportMetrics metrics = new LeakReportMetrics();
        LeakReportJsonFileReporter fileReporter = new LeakReportJsonFileReporter(outputFile);
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                new Slf4jLeakReporter(),
                metrics,
                fileReporter
            )
        );

        ObservableSampleResource cleaned = new ObservableSampleResource("cleaned-observable");
        cleaned.close();

        new ObservableSampleResource("leaked-observable");

        System.gc();
        Thread.sleep(1500L);

        LeakMetricsSnapshot snapshot = metrics.snapshot();
        System.out.println("LeakWatch metrics totalReports=" + snapshot.totalReports());
        System.out.println("LeakWatch JSON report file=" + outputFile.toAbsolutePath());
    }
}
