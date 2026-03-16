package cafe.woden.leakwatch.metrics;

import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakReporter;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory counters derived from incoming {@link cafe.woden.leakwatch.core.LeakReport} values.
 */
public final class LeakReportMetrics implements LeakReporter {
    private final AtomicLong totalReports = new AtomicLong();
    private final ConcurrentMap<LeakReportType, AtomicLong> countsByType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> countsByClass = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<LeakReportType, AtomicLong>> countsByClassAndType = new ConcurrentHashMap<>();

    @Override
    public void report(LeakReport report) {
        totalReports.incrementAndGet();
        countsByType.computeIfAbsent(report.type(), ignored -> new AtomicLong()).incrementAndGet();
        countsByClass.computeIfAbsent(report.className(), ignored -> new AtomicLong()).incrementAndGet();
        countsByClassAndType
            .computeIfAbsent(report.className(), ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(report.type(), ignored -> new AtomicLong())
            .incrementAndGet();
    }

    /**
     * Returns the total number of reports seen.
     */
    public long totalReports() {
        return totalReports.get();
    }

    /**
     * Returns the count for one report type.
     */
    public long count(LeakReportType type) {
        AtomicLong count = countsByType.get(type);
        return count == null ? 0L : count.get();
    }

    /**
     * Returns the total count for one tracked class name.
     */
    public long countForClass(String className) {
        AtomicLong count = countsByClass.get(className);
        return count == null ? 0L : count.get();
    }

    /**
     * Returns the count for one tracked class name and report type pair.
     */
    public long countForClassAndType(String className, LeakReportType type) {
        Map<LeakReportType, AtomicLong> byType = countsByClassAndType.get(className);
        if (byType == null) {
            return 0L;
        }
        AtomicLong count = byType.get(type);
        return count == null ? 0L : count.get();
    }

    /**
     * Returns an immutable snapshot of the current counters.
     */
    public LeakMetricsSnapshot snapshot() {
        EnumMap<LeakReportType, Long> typeSnapshot = new EnumMap<>(LeakReportType.class);
        for (LeakReportType type : LeakReportType.values()) {
            typeSnapshot.put(type, count(type));
        }

        LinkedHashMap<String, Long> classSnapshot = new LinkedHashMap<>();
        countsByClass.forEach((className, count) -> classSnapshot.put(className, count.get()));

        LinkedHashMap<String, Map<LeakReportType, Long>> classAndTypeSnapshot = new LinkedHashMap<>();
        countsByClassAndType.forEach((className, byType) -> {
            EnumMap<LeakReportType, Long> typeCounts = new EnumMap<>(LeakReportType.class);
            for (LeakReportType type : LeakReportType.values()) {
                AtomicLong count = byType.get(type);
                typeCounts.put(type, count == null ? 0L : count.get());
            }
            classAndTypeSnapshot.put(className, Map.copyOf(typeCounts));
        });

        return new LeakMetricsSnapshot(
            totalReports(),
            Map.copyOf(typeSnapshot),
            Map.copyOf(classSnapshot),
            Map.copyOf(classAndTypeSnapshot)
        );
    }

    public void reset() {
        totalReports.set(0L);
        countsByType.clear();
        countsByClass.clear();
        countsByClassAndType.clear();
    }
}
