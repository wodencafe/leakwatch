package cafe.woden.leakwatch.sample.retention;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoBackendRetentionIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(LeakWatchConfig.defaults(), reporter);
    }

    @Test
    void defaultsToUnsupportedEstimatorWhenNoOptionalBackendIsPresent() {
        assertFalse(ShallowSizeEstimators.autoDiscover().isAvailable());
        assertEquals("unsupported", ShallowSizeEstimators.autoDiscover().description());
    }

    @Test
    void byteBudgetDoesNotEmitWhenNoOptionalBackendIsPresent() {
        new NoBackendRetentionTrackedObject();
        new NoBackendRetentionTrackedObject();

        assertTrue(reporter.snapshot().stream().noneMatch(report -> report.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED));
    }
}
