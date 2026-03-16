package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakWatchReporterSystemPropertiesTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsToSlf4jReporterWhenNoReporterPropertyIsPresent() {
        LeakReporter reporter = LeakWatchReporterSystemProperties.load(new Properties());
        assertInstanceOf(Slf4jLeakReporter.class, reporter);
    }

    @Test
    void jsonReporterWritesToConfiguredPath() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "json");
        properties.setProperty(LeakWatchReporterSystemProperties.JSON_PATH, output.toString());

        LeakReporter reporter = LeakWatchReporterSystemProperties.load(properties);
        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP));

        List<String> lines = Files.readAllLines(output);
        assertTrue(lines.get(0).contains("\"id\":1"));
    }

    @Test
    void rollingJsonReporterRotatesUsingConfiguredSettings() throws Exception {
        Path output = tempDir.resolve("reports/leakwatch.ndjson");
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "rolling-json");
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH, output.toString());
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES, "1");
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_ARCHIVES, "1");

        LeakReporter reporter = LeakWatchReporterSystemProperties.load(properties);
        reporter.report(sampleReport(2L, LeakReportType.GC_WITHOUT_CLEANUP));
        reporter.report(sampleReport(3L, LeakReportType.FALLBACK_CLEANUP_FAILED));

        assertTrue(Files.exists(output));
        assertTrue(Files.exists(output.resolveSibling("leakwatch.ndjson.1")));
    }

    @Test
    void environmentVariablesCanDriveReporterSelection() throws Exception {
        Path jsonOutput = tempDir.resolve("reports/from-env.ndjson");

        LeakReporter reporter = LeakWatchReporterSystemProperties.load(
            new Properties(),
            Map.of(
                "LEAKWATCH_REPORTERS", "json",
                "LEAKWATCH_JSON_PATH", jsonOutput.toString()
            )
        );
        reporter.report(sampleReport(5L, LeakReportType.GC_WITHOUT_CLEANUP));

        assertTrue(Files.exists(jsonOutput));
    }

    @Test
    void systemPropertiesOverrideEnvironmentVariablesForReporterSelection() throws Exception {
        Path jsonOutput = tempDir.resolve("reports/from-properties.ndjson");
        Path rollingOutput = tempDir.resolve("reports/from-env-rolling.ndjson");
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "json");
        properties.setProperty(LeakWatchReporterSystemProperties.JSON_PATH, jsonOutput.toString());

        LeakReporter reporter = LeakWatchReporterSystemProperties.load(
            properties,
            Map.of(
                "LEAKWATCH_REPORTERS", "rolling-json",
                "LEAKWATCH_ROLLING_JSON_PATH", rollingOutput.toString(),
                "LEAKWATCH_ROLLING_JSON_MAX_BYTES", "1"
            )
        );
        reporter.report(sampleReport(6L, LeakReportType.GC_WITHOUT_CLEANUP));

        assertTrue(Files.exists(jsonOutput));
        assertTrue(Files.notExists(rollingOutput));
    }

    @Test
    void compositeConfigurationCanWriteToJsonAndRollingJsonSinks() throws Exception {
        Path jsonOutput = tempDir.resolve("reports/leakwatch.ndjson");
        Path rollingOutput = tempDir.resolve("rolling/leakwatch.ndjson");
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "json,rolling-json");
        properties.setProperty(LeakWatchReporterSystemProperties.JSON_PATH, jsonOutput.toString());
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH, rollingOutput.toString());
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES, "4096");

        LeakReporter reporter = LeakWatchReporterSystemProperties.load(properties);
        reporter.report(sampleReport(4L, LeakReportType.STRICT_MODE_WARNING));

        assertTrue(Files.exists(jsonOutput));
        assertTrue(Files.exists(rollingOutput));
    }

    @Test
    void missingJsonPathFailsFast() {
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "json");

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> LeakWatchReporterSystemProperties.load(properties)
        );

        assertTrue(thrown.getMessage().contains(LeakWatchReporterSystemProperties.JSON_PATH));
    }

    @Test
    void invalidReporterIdFailsFast() {
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "sparkles");

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> LeakWatchReporterSystemProperties.load(properties)
        );

        assertTrue(thrown.getMessage().contains("sparkles"));
    }

    @Test
    void invalidRollingJsonMaxBytesFailsFast() {
        Path output = tempDir.resolve("rolling/leakwatch.ndjson");
        Properties properties = new Properties();
        properties.setProperty(LeakWatchReporterSystemProperties.REPORTERS, "rolling-json");
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_PATH, output.toString());
        properties.setProperty(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES, "0");

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> LeakWatchReporterSystemProperties.load(properties)
        );

        assertTrue(thrown.getMessage().contains(LeakWatchReporterSystemProperties.ROLLING_JSON_MAX_BYTES));
    }

    private static LeakReport sampleReport(long id, LeakReportType type) {
        return new LeakReport(
            type,
            id,
            "example.ConfiguredTrackedThing",
            Set.of("close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(123L),
            Set.of("config", "reporter"),
            null,
            "sample message",
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "example.DeletePathFallbackCleanup" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "java.io.IOException" : null,
            type == LeakReportType.FALLBACK_CLEANUP_FAILED ? "boom" : null
        );
    }
}
