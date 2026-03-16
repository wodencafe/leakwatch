package cafe.woden.leakwatch.core;

import java.util.Objects;
import java.util.Set;

/**
 * Runtime configuration for LeakWatch.
 * <p>
 * This covers the main on/off and behavior switches used by the core registry.
 */
public record LeakWatchConfig(
    boolean enabled,
    boolean defaultCaptureStackTraces,
    boolean strictMode,
    boolean defaultConventionCleanupDetection,
    Set<String> excludedPackages,
    RetainedAfterCleanupDiagnosticHook retainedAfterCleanupDiagnosticHook
) {
    public LeakWatchConfig(
        boolean enabled,
        boolean defaultCaptureStackTraces,
        boolean strictMode,
        boolean defaultConventionCleanupDetection,
        Set<String> excludedPackages
    ) {
        this(
            enabled,
            defaultCaptureStackTraces,
            strictMode,
            defaultConventionCleanupDetection,
            excludedPackages,
            RetainedAfterCleanupDiagnosticHook.noop()
        );
    }

    public LeakWatchConfig {
        excludedPackages = Set.copyOf(Objects.requireNonNull(excludedPackages, "excludedPackages"));
        retainedAfterCleanupDiagnosticHook = Objects.requireNonNull(
            retainedAfterCleanupDiagnosticHook,
            "retainedAfterCleanupDiagnosticHook"
        );
    }

    /**
     * Returns the default LeakWatch configuration.
     */
    public static LeakWatchConfig defaults() {
        return new LeakWatchConfig(
            true,
            false,
            false,
            true,
            Set.of(
                "java.",
                "javax.",
                "jdk.",
                "sun.",
                "cafe.woden.leakwatch.core.",
                "cafe.woden.leakwatch.aspectj.",
                "cafe.woden.leakwatch.annotations."
            ),
            RetainedAfterCleanupDiagnosticHook.noop()
        );
    }

    /**
     * Returns {@code true} when the given class name falls under one of the excluded package prefixes.
     */
    public boolean isExcluded(String className) {
        return excludedPackages.stream().anyMatch(className::startsWith);
    }

    /**
     * Returns a copy of this configuration with a retained-after-cleanup diagnostic hook attached.
     */
    public LeakWatchConfig withRetainedAfterCleanupDiagnosticHook(RetainedAfterCleanupDiagnosticHook diagnosticHook) {
        return new LeakWatchConfig(
            enabled,
            defaultCaptureStackTraces,
            strictMode,
            defaultConventionCleanupDetection,
            excludedPackages,
            Objects.requireNonNull(diagnosticHook, "diagnosticHook")
        );
    }

    /**
     * Returns {@code true} when a diagnostic hook other than the default no-op hook is configured.
     */
    public boolean hasRetainedAfterCleanupDiagnosticHook() {
        return retainedAfterCleanupDiagnosticHook != NoopRetainedAfterCleanupDiagnosticHook.INSTANCE;
    }
}
