package cafe.woden.leakwatch.jfr;

import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakReporter;

/**
 * {@link cafe.woden.leakwatch.core.LeakReporter} that converts LeakWatch reports into JFR events.
 */
public final class JfrLeakReporter implements LeakReporter {
    @Override
    public void report(LeakReport report) {
        switch (report.type()) {
            case GC_WITHOUT_CLEANUP -> commit(report, new LeakGcWithoutCleanupEvent());
            case RETENTION_COUNT_EXCEEDED -> commit(report, new RetentionCountExceededEvent());
            case RETENTION_APPROX_BYTES_EXCEEDED -> commit(report, new RetentionApproxBytesExceededEvent());
            case FALLBACK_CLEANUP_EXECUTED -> commit(report, new FallbackCleanupExecutedEvent());
            case FALLBACK_CLEANUP_FAILED -> commit(report, new FallbackCleanupFailedEvent());
            case RETAINED_AFTER_CLEANUP -> commit(report, new RetainedAfterCleanupEvent());
            case STRICT_MODE_WARNING -> commit(report, new StrictModeWarningEvent());
        }
    }

    private static void commit(LeakReport report, AbstractLeakWatchEvent event) {
        event.populateFrom(report);
        if (event.shouldCommit()) {
            event.commit();
        }
    }
}
