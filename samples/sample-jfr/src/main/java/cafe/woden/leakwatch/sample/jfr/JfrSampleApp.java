package cafe.woden.leakwatch.sample.jfr;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;
import cafe.woden.leakwatch.jfr.JfrLeakReporter;
import jdk.jfr.Recording;

import java.nio.file.Files;
import java.nio.file.Path;

public final class JfrSampleApp {
    private JfrSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path recordingFile = args.length > 0
            ? Path.of(args[0])
            : Path.of("build", "leakwatch", "sample-jfr-recording.jfr");

        Path parent = recordingFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                new Slf4jLeakReporter(),
                new JfrLeakReporter()
            )
        );

        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.start();

            JfrSampleResource cleaned = new JfrSampleResource("cleaned-jfr");
            cleaned.close();

            new JfrSampleResource("leaked-jfr");

            System.gc();
            Thread.sleep(1500L);

            recording.stop();
            recording.dump(recordingFile);
        }

        System.out.println("LeakWatch JFR recording=" + recordingFile.toAbsolutePath());
    }
}
