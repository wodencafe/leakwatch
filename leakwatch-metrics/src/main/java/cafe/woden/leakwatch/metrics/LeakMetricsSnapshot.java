package cafe.woden.leakwatch.metrics;

import cafe.woden.leakwatch.core.LeakReportType;

import java.util.Map;

/**
 * Immutable snapshot of counters collected by {@link LeakReportMetrics}.
 */
public record LeakMetricsSnapshot(
    long totalReports,
    Map<LeakReportType, Long> countsByType,
    Map<String, Long> countsByClass,
    Map<String, Map<LeakReportType, Long>> countsByClassAndType
) {
}
