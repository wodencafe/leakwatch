package cafe.woden.leakwatch.core;

/**
 * Kinds of reports LeakWatch can emit.
 */
public enum LeakReportType {
    GC_WITHOUT_CLEANUP,
    RETENTION_COUNT_EXCEEDED,
    RETENTION_APPROX_BYTES_EXCEEDED,
    FALLBACK_CLEANUP_EXECUTED,
    FALLBACK_CLEANUP_FAILED,
    RETAINED_AFTER_CLEANUP,
    STRICT_MODE_WARNING
}
