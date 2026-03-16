package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.jfr.JfrLeakReporter;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictModeJfrIntegrationTest {
    @Test
    void strictModeWarningIsEmittedToJfr() throws Exception {
        LeakWatchConfig defaults = LeakWatchConfig.defaults();
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(
            new LeakWatchConfig(
                defaults.enabled(),
                defaults.defaultCaptureStackTraces(),
                true,
                defaults.defaultConventionCleanupDetection(),
                defaults.excludedPackages()
            ),
            new CompositeLeakReporter(reporter, new JfrLeakReporter())
        );

        Path recordingFile = Files.createTempFile("leakwatch-strict", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.StrictModeWarning");
            recording.start();

            new NoCleanupTrackedResource("first");
            new NoCleanupTrackedResource("second");

            recording.stop();
            recording.dump(recordingFile);
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
        List<RecordedEvent> strictWarnings = events.stream()
            .filter(event -> event.getEventType().getName().equals("cafe.woden.leakwatch.StrictModeWarning"))
            .toList();

        assertEquals(1, strictWarnings.size());
        RecordedEvent event = strictWarnings.get(0);
        assertEquals(NoCleanupTrackedResource.class.getName(), event.getString("className"));
        assertTrue(event.getString("message").contains("was not lifecycle-tracked"));
        assertEquals(1, reporter.snapshot().size());
    }
}
