package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.Severity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Structured metadata supplied to {@link RetainedAfterCleanupDiagnosticHook}.
 * <p>
 * The context deliberately mirrors the most useful retained-after-cleanup report fields so diagnostic integrations can
 * work from stable, typed data rather than reparsing log lines.
 * <p>
 * It still does <strong>not</strong> prove the exact retaining owner. Implementations that need the retaining path
 * should escalate to tooling such as JFR old-object sampling, heap dumps, or other JVM diagnostics.
 *
 * @param report retained-after-cleanup report that triggered the hook
 */
public record RetainedAfterCleanupDiagnosticContext(LeakReport report) {
    public RetainedAfterCleanupDiagnosticContext {
        Objects.requireNonNull(report, "report");
        if (report.type() != LeakReportType.RETAINED_AFTER_CLEANUP) {
            throw new IllegalArgumentException(
                "RetainedAfterCleanupDiagnosticContext requires a RETAINED_AFTER_CLEANUP report but got " + report.type()
            );
        }
    }

    public static RetainedAfterCleanupDiagnosticContext from(LeakReport report) {
        return new RetainedAfterCleanupDiagnosticContext(report);
    }

    public long id() {
        return report.id();
    }

    public String className() {
        return report.className();
    }

    public Set<String> expectedCleanupMethods() {
        return report.expectedCleanupMethods();
    }

    public String observedCleanupMethodName() {
        return report.observedCleanupMethodName();
    }

    public Instant createdAt() {
        return report.createdAt();
    }

    public Duration age() {
        return report.age();
    }

    public Set<String> tags() {
        return report.tags();
    }

    public Throwable allocationSite() {
        return report.allocationSite();
    }

    public String message() {
        return report.message();
    }

    public Severity severity() {
        return report.severity();
    }

    public Instant cleanedAt() {
        return report.cleanedAt();
    }

    public Duration ageSinceCleanup() {
        return report.ageSinceCleanup();
    }

    public Throwable cleanupSite() {
        return report.cleanupSite();
    }

    public Long postCleanupGraceMillis() {
        return report.postCleanupGraceMillis();
    }
}
