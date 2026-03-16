package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.Severity;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable payload describing one LeakWatch signal.
 * <p>
 * Reporters receive this object and can log it, publish metrics from it, or write it to another sink.
 */
public record LeakReport(
    LeakReportType type,
    long id,
    String className,
    Set<String> expectedCleanupMethods,
    String observedCleanupMethodName,
    Instant createdAt,
    Duration age,
    Set<String> tags,
    Throwable allocationSite,
    String message,
    Severity severity,
    String fallbackActionClassName,
    String failureClassName,
    String failureMessage,
    Long retentionLiveCount,
    Long retentionMaxLiveInstances,
    Long retentionApproxShallowBytes,
    Long retentionMaxApproxShallowBytes,
    Long retentionApproxBytesOverBudget,
    Instant cleanedAt,
    Duration ageSinceCleanup,
    Throwable cleanupSite,
    Long postCleanupGraceMillis
) {
    public LeakReport {
        expectedCleanupMethods = canonicalize(expectedCleanupMethods);
        tags = canonicalize(tags);
        severity = severity == null ? Severity.WARN : severity;
    }

    public LeakReport(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Duration age,
        Set<String> tags,
        Throwable allocationSite,
        String message
    ) {
        this(
            type,
            id,
            className,
            expectedCleanupMethods,
            observedCleanupMethodName,
            createdAt,
            age,
            tags,
            allocationSite,
            message,
            defaultSeverity(type),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public LeakReport(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Duration age,
        Set<String> tags,
        Throwable allocationSite,
        String message,
        String fallbackActionClassName,
        String failureClassName,
        String failureMessage
    ) {
        this(
            type,
            id,
            className,
            expectedCleanupMethods,
            observedCleanupMethodName,
            createdAt,
            age,
            tags,
            allocationSite,
            message,
            defaultSeverity(type),
            fallbackActionClassName,
            failureClassName,
            failureMessage,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public LeakReport(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Duration age,
        Set<String> tags,
        Throwable allocationSite,
        String message,
        Severity severity
    ) {
        this(
            type,
            id,
            className,
            expectedCleanupMethods,
            observedCleanupMethodName,
            createdAt,
            age,
            tags,
            allocationSite,
            message,
            severity,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public LeakReport(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Duration age,
        Set<String> tags,
        Throwable allocationSite,
        String message,
        Severity severity,
        Long retentionLiveCount,
        Long retentionMaxLiveInstances,
        Long retentionApproxShallowBytes,
        Long retentionMaxApproxShallowBytes,
        Long retentionApproxBytesOverBudget
    ) {
        this(
            type,
            id,
            className,
            expectedCleanupMethods,
            observedCleanupMethodName,
            createdAt,
            age,
            tags,
            allocationSite,
            message,
            severity,
            null,
            null,
            null,
            retentionLiveCount,
            retentionMaxLiveInstances,
            retentionApproxShallowBytes,
            retentionMaxApproxShallowBytes,
            retentionApproxBytesOverBudget,
            null,
            null,
            null,
            null
        );
    }

    public LeakReport(
        LeakReportType type,
        long id,
        String className,
        Set<String> expectedCleanupMethods,
        String observedCleanupMethodName,
        Instant createdAt,
        Duration age,
        Set<String> tags,
        Throwable allocationSite,
        String message,
        Severity severity,
        Instant cleanedAt,
        Duration ageSinceCleanup,
        Throwable cleanupSite,
        Long postCleanupGraceMillis
    ) {
        this(
            type,
            id,
            className,
            expectedCleanupMethods,
            observedCleanupMethodName,
            createdAt,
            age,
            tags,
            allocationSite,
            message,
            severity,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            cleanedAt,
            ageSinceCleanup,
            cleanupSite,
            postCleanupGraceMillis
        );
    }

    /**
     * Returns the expected cleanup methods as a comma-separated string.
     */
    public String expectedCleanupMethodsCsv() {
        return String.join(",", expectedCleanupMethods);
    }

    /**
     * Returns the report tags as a comma-separated string.
     */
    public String tagsCsv() {
        return String.join(",", tags);
    }

    private static Severity defaultSeverity(LeakReportType type) {
        return switch (type) {
            case FALLBACK_CLEANUP_EXECUTED -> Severity.INFO;
            case FALLBACK_CLEANUP_FAILED -> Severity.ERROR;
            default -> Severity.WARN;
        };
    }

    private static Set<String> canonicalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> canonical = values.stream()
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (canonical.isEmpty()) {
            return Set.of();
        }

        return Collections.unmodifiableSet(canonical);
    }
}
