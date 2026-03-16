package cafe.woden.leakwatch.sample.jfr;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.jfr.JfrLeakReporter;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JfrSampleIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void leakedTrackedObjectCreatesJfrRecording() throws Exception {
        InMemoryLeakReporter inMemory = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(inMemory, new JfrLeakReporter())
        );

        Path recordingFile = tempDir.resolve("sample-jfr-recording.jfr");
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.start();

            WeakReference<JfrSampleResource> reference = createLeakedResource();
            LeakReport report = GcAwaiter.awaitReport(
                reference,
                inMemory::snapshot,
                candidate -> candidate.type() == LeakReportType.GC_WITHOUT_CLEANUP,
                Duration.ofSeconds(10)
            );

            recording.stop();
            recording.dump(recordingFile);

            List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
            RecordedEvent event = events.stream()
                .filter(candidate -> candidate.getEventType().getName().equals("cafe.woden.leakwatch.LeakGcWithoutCleanup"))
                .findFirst()
                .orElseThrow();

            assertEquals(report.className(), event.getString("className"));
            assertEquals(report.expectedCleanupMethodsCsv(), event.getString("expectedCleanupMethods"));
            assertEquals(report.tagsCsv(), event.getString("tags"));
            assertTrue(event.getString("message").contains("garbage collected before an explicit cleanup method"));
        }
    }

    private WeakReference<JfrSampleResource> createLeakedResource() {
        JfrSampleResource leaked = new JfrSampleResource("integration-leaked-jfr");
        return new WeakReference<>(leaked);
    }
}
