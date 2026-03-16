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

class FallbackCleanupFailureJfrIntegrationTest {
    @Test
    void leakedTrackedTempFileEmitsFallbackFailureJfrEvent() throws Exception {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(reporter, new JfrLeakReporter())
        );

        Path recordingFile = Files.createTempFile("leakwatch-fallback-failure", ".jfr");
        Path leakedPath = null;
        try (Recording recording = new Recording()) {
            recording.enable("cafe.woden.leakwatch.LeakGcWithoutCleanup");
            recording.enable("cafe.woden.leakwatch.FallbackCleanupFailed");
            recording.start();

            LeakedBrokenTrackedTempFile leaked = createLeakedBrokenTrackedTempFile("leakwatch-jfr-broken-fallback");
            leakedPath = leaked.path();

            LeakReport failureReport = GcAwaiter.awaitReport(
                leaked.reference(),
                reporter::snapshot,
                report -> report.type() == LeakReportType.FALLBACK_CLEANUP_FAILED
                    && report.className().equals(BrokenTrackedTempFile.class.getName()),
                Duration.ofSeconds(10)
            );

            recording.stop();
            recording.dump(recordingFile);

            List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
            RecordedEvent event = events.stream()
                .filter(candidate -> candidate.getEventType().getName().equals("cafe.woden.leakwatch.FallbackCleanupFailed"))
                .findFirst()
                .orElseThrow();

            assertEquals(failureReport.className(), event.getString("className"));
            assertTrue(event.getString("message").contains(ExplodingPathFallbackCleanup.class.getName()));
            assertTrue(event.getString("message").contains("simulated fallback failure"));
            assertEquals(ExplodingPathFallbackCleanup.class.getName(), event.getString("fallbackActionClassName"));
            assertEquals(java.io.IOException.class.getName(), event.getString("failureClassName"));
            assertTrue(event.getString("failureMessage").contains("simulated fallback failure"));
        } finally {
            if (leakedPath != null) {
                Files.deleteIfExists(leakedPath);
            }
        }
    }

    private LeakedBrokenTrackedTempFile createLeakedBrokenTrackedTempFile(String prefix) throws Exception {
        BrokenTrackedTempFile leaked = new BrokenTrackedTempFile(prefix);
        return new LeakedBrokenTrackedTempFile(new WeakReference<>(leaked), leaked.path());
    }

    private record LeakedBrokenTrackedTempFile(WeakReference<BrokenTrackedTempFile> reference, Path path) {
    }
}
