package cafe.woden.leakwatch.annotations;

/**
 * Severity level attached to LeakWatch reports.
 */
public enum Severity {
    /** Informational only. */
    INFO,
    /** Worth attention, but not necessarily fatal. */
    WARN,
    /** Serious enough to treat as an error. */
    ERROR
}
