package cafe.woden.leakwatch.sample.closeable;

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

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseableJfrIntegrationTest {
    @Test
    void leakedTrackedObjectEmitsJfrEventThroughCompositeReporter() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(reporter, new JfrLeakReporter())
        );

        Path recordingFile = Files.createTempFile("leakwatch-closeable", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.start();

            WeakReference<SampleResource> reference = createLeakedResource();
            LeakReport leakReport = GcAwaiter.awaitReport(
                reference,
                reporter::snapshot,
                report -> report.type() == LeakReportType.GC_WITHOUT_CLEANUP,
                Duration.ofSeconds(10)
            );

            recording.stop();
            recording.dump(recordingFile);

            List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
            RecordedEvent event = events.stream()
                .filter(candidate -> candidate.getEventType().getName().equals("cafe.woden.leakwatch.LeakGcWithoutCleanup"))
                .findFirst()
                .orElseThrow();

            assertEquals(leakReport.className(), event.getString("className"));
            assertEquals(String.join(",", leakReport.expectedCleanupMethods()), event.getString("expectedCleanupMethods"));
            assertTrue(event.getString("message").contains("garbage collected before an explicit cleanup method"));
        }
    }

    private WeakReference<SampleResource> createLeakedResource() {
        SampleResource leaked = new SampleResource("jfr-leaked-test");
        return new WeakReference<>(leaked);
    }
}
