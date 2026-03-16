package cafe.woden.leakwatch.sample.fallback;

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

class FallbackCleanupJfrIntegrationTest {
    @Test
    void leakedTrackedTempFileEmitsFallbackJfrEvent() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(reporter, new JfrLeakReporter())
        );

        Path recordingFile = Files.createTempFile("leakwatch-fallback", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.enable("cafe.woden.leakwatch.FallbackCleanupExecuted");
            recording.start();

            WeakReference<TrackedTempFile> reference = createLeakedTrackedTempFile("leakwatch-jfr-fallback");

            LeakReport fallbackReport = GcAwaiter.awaitReport(
                reference,
                reporter::snapshot,
                report -> report.type() == LeakReportType.FALLBACK_CLEANUP_EXECUTED,
                Duration.ofSeconds(10)
            );

            recording.stop();
            recording.dump(recordingFile);

            List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
            RecordedEvent event = events.stream()
                .filter(candidate -> candidate.getEventType().getName().equals("cafe.woden.leakwatch.FallbackCleanupExecuted"))
                .findFirst()
                .orElseThrow();

            assertEquals(fallbackReport.className(), event.getString("className"));
            assertTrue(event.getString("message").contains(DeletePathFallbackCleanup.class.getName()));
        }
    }

    private WeakReference<TrackedTempFile> createLeakedTrackedTempFile(String prefix) throws Exception {
        TrackedTempFile leaked = new TrackedTempFile(prefix);
        return new WeakReference<>(leaked);
    }
}
