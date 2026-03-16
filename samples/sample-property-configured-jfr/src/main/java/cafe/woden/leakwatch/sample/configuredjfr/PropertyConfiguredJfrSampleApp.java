package cafe.woden.leakwatch.sample.configuredjfr;

import cafe.woden.leakwatch.core.LeakWatchRuntime;
import jdk.jfr.Recording;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PropertyConfiguredJfrSampleApp {
    private PropertyConfiguredJfrSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path recordingFile = args.length > 0
            ? Path.of(args[0])
            : Path.of("build", "leakwatch", "sample-configured-jfr-recording.jfr");

        Path parent = recordingFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        LeakWatchRuntime.configureFromSystemPropertiesAndEnvironment();

        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.start();

            PropertyConfiguredJfrSampleResource cleaned = new PropertyConfiguredJfrSampleResource("cleaned-configured-jfr");
            cleaned.close();

            new PropertyConfiguredJfrSampleResource("leaked-configured-jfr");

            System.gc();
            Thread.sleep(1500L);

            recording.stop();
            recording.dump(recordingFile);
        }

        System.out.println("LeakWatch configured JFR recording=" + recordingFile.toAbsolutePath());
    }
}
