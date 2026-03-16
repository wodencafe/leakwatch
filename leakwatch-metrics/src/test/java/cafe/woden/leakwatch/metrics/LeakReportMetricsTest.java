package cafe.woden.leakwatch.metrics;

import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeakReportMetricsTest {
    @Test
    void reporterAggregatesCountsByTypeAndClass() {
        LeakReportMetrics metrics = new LeakReportMetrics();

        metrics.report(report(LeakReportType.GC_WITHOUT_CLEANUP, 1L, "example.SocketHandle"));
        metrics.report(report(LeakReportType.FALLBACK_CLEANUP_EXECUTED, 1L, "example.SocketHandle"));
        metrics.report(report(LeakReportType.FALLBACK_CLEANUP_FAILED, 2L, "example.TempFile"));
        metrics.report(report(LeakReportType.STRICT_MODE_WARNING, -1L, "example.TempFile"));

        LeakMetricsSnapshot snapshot = metrics.snapshot();

        assertEquals(4L, metrics.totalReports());
        assertEquals(4L, snapshot.totalReports());
        assertEquals(1L, metrics.count(LeakReportType.GC_WITHOUT_CLEANUP));
        assertEquals(1L, metrics.count(LeakReportType.FALLBACK_CLEANUP_EXECUTED));
        assertEquals(1L, metrics.count(LeakReportType.FALLBACK_CLEANUP_FAILED));
        assertEquals(1L, metrics.count(LeakReportType.STRICT_MODE_WARNING));
        assertEquals(0L, metrics.count(LeakReportType.RETENTION_COUNT_EXCEEDED));

        assertEquals(2L, metrics.countForClass("example.SocketHandle"));
        assertEquals(2L, metrics.countForClass("example.TempFile"));
        assertEquals(1L, metrics.countForClassAndType("example.SocketHandle", LeakReportType.GC_WITHOUT_CLEANUP));
        assertEquals(1L, metrics.countForClassAndType("example.SocketHandle", LeakReportType.FALLBACK_CLEANUP_EXECUTED));
        assertEquals(1L, metrics.countForClassAndType("example.TempFile", LeakReportType.FALLBACK_CLEANUP_FAILED));
        assertEquals(1L, metrics.countForClassAndType("example.TempFile", LeakReportType.STRICT_MODE_WARNING));
        assertEquals(0L, metrics.countForClassAndType("example.SocketHandle", LeakReportType.STRICT_MODE_WARNING));

        assertEquals(0L, snapshot.countsByType().get(LeakReportType.RETENTION_COUNT_EXCEEDED));
        assertEquals(1L, snapshot.countsByClassAndType().get("example.TempFile").get(LeakReportType.FALLBACK_CLEANUP_FAILED));
    }

    @Test
    void resetClearsAllAggregates() {
        LeakReportMetrics metrics = new LeakReportMetrics();
        metrics.report(report(LeakReportType.GC_WITHOUT_CLEANUP, 1L, "example.SocketHandle"));

        metrics.reset();

        LeakMetricsSnapshot snapshot = metrics.snapshot();
        assertEquals(0L, metrics.totalReports());
        assertEquals(0L, snapshot.totalReports());
        for (LeakReportType type : LeakReportType.values()) {
            assertEquals(0L, snapshot.countsByType().get(type));
        }
        assertEquals(0, snapshot.countsByClass().size());
        assertEquals(0, snapshot.countsByClassAndType().size());
    }

    private static LeakReport report(LeakReportType type, long id, String className) {
        return new LeakReport(
            type,
            id,
            className,
            Set.of("close"),
            null,
            Instant.now(),
            Duration.ofMillis(10L),
            Set.of("sample"),
            null,
            type.name()
        );
    }
}
