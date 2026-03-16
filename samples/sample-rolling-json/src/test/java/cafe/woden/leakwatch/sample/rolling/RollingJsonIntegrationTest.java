package cafe.woden.leakwatch.sample.rolling;

import cafe.woden.leakwatch.core.LeakWatchReporterSystemProperties;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollingJsonIntegrationTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty(LeakWatchReporterSystemProperties.REPORTERS);
        System.clearProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH);
        System.clearProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES);
        System.clearProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_ARCHIVES);
    }

    @Test
    void configureFromSystemPropertiesCanDriveRollingJsonOutput() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");

        System.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "rolling-json");
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH, output.toString());
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES, "1");
        System.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_ARCHIVES, "1");

        LeakWatchRuntime.configureFromSystemProperties();

        List<WeakReference<RollingJsonSampleResource>> references = createLeakedResources();

        GcAwaiter.awaitCondition(
            Duration.ofSeconds(10),
            () -> references.stream().allMatch(reference -> reference.get() == null)
                && Files.exists(output)
                && Files.exists(output.resolveSibling("leakwatch.ndjson.1")),
            () -> "Expected rolling NDJSON output and first archive to exist under " + output
        );

        List<String> currentLines = Files.readAllLines(output);
        List<String> archiveLines = Files.readAllLines(output.resolveSibling("leakwatch.ndjson.1"));

        assertEquals(1, currentLines.size());
        assertEquals(1, archiveLines.size());
        assertTrue(currentLines.get(0).contains("\"type\":\"GC_WITHOUT_CLEANUP\""));
        assertTrue(archiveLines.get(0).contains("\"type\":\"GC_WITHOUT_CLEANUP\""));
        assertTrue(currentLines.get(0).contains(RollingJsonSampleResource.class.getName()));
        assertTrue(archiveLines.get(0).contains(RollingJsonSampleResource.class.getName()));
    }

    private static List<WeakReference<RollingJsonSampleResource>> createLeakedResources() {
        List<WeakReference<RollingJsonSampleResource>> references = new ArrayList<>();
        references.add(createLeakedResource("one"));
        references.add(createLeakedResource("two"));
        return List.copyOf(references);
    }

    private static WeakReference<RollingJsonSampleResource> createLeakedResource(String name) {
        RollingJsonSampleResource leaked = new RollingJsonSampleResource(name);
        return new WeakReference<>(leaked);
    }
}
