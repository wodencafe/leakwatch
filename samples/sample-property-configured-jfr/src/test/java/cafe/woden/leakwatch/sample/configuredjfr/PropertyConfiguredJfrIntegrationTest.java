package cafe.woden.leakwatch.sample.configuredjfr;

import cafe.woden.leakwatch.core.LeakWatchReporterSystemProperties;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyConfiguredJfrIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void configureFromSystemPropertiesCanCreateJfrReporterReflectively() throws Exception {
        Path jsonOutput = tempDir.resolve("reports/leakwatch.ndjson");
        Path recordingFile = tempDir.resolve("recordings/configured-jfr.jfr");
        Files.createDirectories(recordingFile.getParent());

        SystemPropertyScope scope = new SystemPropertyScope()
            .set(LeakWatchReporterSystemProperties.REPORTERS, "json,jfr")
            .set(LeakWatchReporterSystemProperties.JSON_PATH, jsonOutput.toString());

        try (scope; Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.start();

            LeakWatchRuntime.configureFromSystemProperties();
            WeakReference<PropertyConfiguredJfrSampleResource> reference = createLeakedResource();

            GcAwaiter.awaitCondition(
                Duration.ofSeconds(10),
                () -> reportWritten(jsonOutput),
                () -> "Timed out waiting for property-configured JSON leak report at " + jsonOutput
            );

            recording.stop();
            recording.dump(recordingFile);
            assertTrue(reference.get() == null || Files.size(jsonOutput) > 0L);
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
        RecordedEvent event = events.stream()
            .filter(candidate -> candidate.getEventType().getName().equals("cafe.woden.leakwatch.LeakGcWithoutCleanup"))
            .findFirst()
            .orElseThrow();

        assertEquals(PropertyConfiguredJfrSampleResource.class.getName(), event.getString("className"));
        assertEquals("close", event.getString("expectedCleanupMethods"));
        assertEquals("configured-jfr,sample", event.getString("tags"));
        assertTrue(event.getString("message").contains("garbage collected before an explicit cleanup method"));
    }

    private WeakReference<PropertyConfiguredJfrSampleResource> createLeakedResource() {
        PropertyConfiguredJfrSampleResource leaked = new PropertyConfiguredJfrSampleResource("integration-configured-jfr");
        return new WeakReference<>(leaked);
    }

    private static boolean reportWritten(Path jsonOutput) {
        try {
            return Files.exists(jsonOutput)
                && Files.readString(jsonOutput).contains(PropertyConfiguredJfrSampleResource.class.getName());
        } catch (Exception exception) {
            return false;
        }
    }

    private static final class SystemPropertyScope implements AutoCloseable {
        private final java.util.Map<String, String> previousValues = new java.util.LinkedHashMap<>();

        SystemPropertyScope set(String key, String value) {
            previousValues.putIfAbsent(key, System.getProperty(key));
            System.setProperty(key, value);
            return this;
        }

        @Override
        public void close() {
            for (java.util.Map.Entry<String, String> entry : previousValues.entrySet()) {
                if (entry.getValue() == null) {
                    System.clearProperty(entry.getKey());
                } else {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
