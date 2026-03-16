package cafe.woden.leakwatch.sample.retention;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @Test
    void retentionThresholdCrossingEmitsSingleReport() {
        List<RetentionTrackedObject> objects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            objects.add(new RetentionTrackedObject());
        }

        List<LeakReport> reports = reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.RETENTION_COUNT_EXCEEDED)
            .toList();

        assertEquals(1, reports.size());
        LeakReport report = reports.get(0);
        assertEquals(RetentionTrackedObject.class.getName(), report.className());
        assertEquals(Set.of(), report.expectedCleanupMethods());
        assertNull(report.observedCleanupMethodName());
        assertTrue(report.message().contains("liveCount=4") || report.message().contains("liveCount=5"));
    }
}
