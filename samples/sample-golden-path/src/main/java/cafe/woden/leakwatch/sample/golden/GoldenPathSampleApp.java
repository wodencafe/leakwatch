package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.LeakReportJsonFileReporter;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;

import java.nio.file.Path;
import java.util.List;

public final class GoldenPathSampleApp {
    private GoldenPathSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path outputFile = args.length > 0
            ? Path.of(args[0])
            : Path.of("build", "leakwatch", "golden-path.ndjson");

        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                new Slf4jLeakReporter(),
                new LeakReportJsonFileReporter(outputFile)
            )
        );

        try (GoldenPathSession session = new GoldenPathSession("cleaned-session")) {
            System.out.println("Closed session explicitly: " + session.name());
        }

        GoldenPathTempFile lease = new GoldenPathTempFile("leakwatch-golden");
        Path leakedPath = lease.path();
        lease = null;

        GoldenPathListenerRegistration listener = new GoldenPathListenerRegistration("zombie-listener");
        listener.close();
        listener = null;

        List<GoldenPathCacheEntry> cacheEntries = List.of(
            new GoldenPathCacheEntry(),
            new GoldenPathCacheEntry(),
            new GoldenPathCacheEntry()
        );

        System.out.println("LeakWatch size estimator: " + ShallowSizeEstimators.autoDiscover().description());
        System.out.println("Retained cache entries: " + cacheEntries.size());
        System.out.println("Retained listener registrations (intentional bug): " + GoldenPathListenerRegistration.retainedCount());
        System.out.println("Leaked temp path (fallback should delete it): " + leakedPath);

        System.gc();
        Thread.sleep(1500L);

        System.out.println("LeakWatch JSON report file=" + outputFile.toAbsolutePath());
        System.out.println("Look for RETAINED_AFTER_CLEANUP with allocationSite + cleanupSite in the NDJSON output.");
    }
}
