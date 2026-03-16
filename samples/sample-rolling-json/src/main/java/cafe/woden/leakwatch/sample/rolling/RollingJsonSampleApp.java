package cafe.woden.leakwatch.sample.rolling;

import cafe.woden.leakwatch.core.LeakWatchReporterSystemProperties;
import cafe.woden.leakwatch.core.LeakWatchRuntime;

import java.nio.file.Path;

public final class RollingJsonSampleApp {
    private RollingJsonSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of("build", "leakwatch", "reports.ndjson");

        System.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "slf4j,rolling-json");
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH, output.toString());
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES, "256");
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_ARCHIVES, "2");

        LeakWatchRuntime.configureFromSystemProperties();

        try (RollingJsonSampleResource cleaned = new RollingJsonSampleResource("cleaned")) {
            System.out.println("Closed resource: " + cleaned.name());
        }

        new RollingJsonSampleResource("leaked-one");
        new RollingJsonSampleResource("leaked-two");

        Thread.sleep(3_000L);
        System.out.println("Rolling NDJSON output written under: " + output.toAbsolutePath());
    }
}
