package cafe.woden.leakwatch.sample.retentionjol;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JolRetentionIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(LeakWatchConfig.defaults(), reporter);
    }

    @Test
    void autoDiscoversJolAndEmitsApproximateByteBudgetReport() {
        assertTrue(ShallowSizeEstimators.autoDiscover().description().contains("leakwatch-jol"));

        new JolRetentionTrackedObject();
        new JolRetentionTrackedObject();
        new JolRetentionTrackedObject();

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.RETENTION_APPROX_BYTES_EXCEEDED)
            .toList();

        assertEquals(1, reports.size());
        LeakReport report = reports.get(0);
        assertEquals(JolRetentionTrackedObject.class.getName(), report.className());
        assertNotNull(report.retentionApproxShallowBytes());
        assertNotNull(report.retentionMaxApproxShallowBytes());
        assertTrue(report.retentionApproxShallowBytes() > report.retentionMaxApproxShallowBytes());
        assertEquals(Long.valueOf(24L), report.retentionMaxApproxShallowBytes());
    }
}
