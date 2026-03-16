package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Emits one-time startup and configuration diagnostics for LeakWatch.
 */
public final class StartupDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final AtomicBoolean startupLogged = new AtomicBoolean(false);
    private final Set<String> leakTrackedClasses = ConcurrentHashMap.newKeySet();
    private final Set<String> retentionTrackedClasses = ConcurrentHashMap.newKeySet();
    private final Set<String> strictWarnings = ConcurrentHashMap.newKeySet();
    private final Set<String> retentionSizingWarnings = ConcurrentHashMap.newKeySet();

    public void logStartupOnce(LeakWatchConfig config) {
        if (startupLogged.compareAndSet(false, true)) {
            log.info(
                "LeakWatch startup: enabled={}, strictMode={}, defaultCaptureStackTraces={}, conventionCleanupDetection={}, retainedAfterCleanupDiagnosticHookEnabled={}, aspectjRuntimePresent={}, leakWatchAspectPresentOnClasspath={}, aspectClassLoaded={}, aspectAdviceObserved={}, lastAdviceKind={}, excludedPackages={}",
                config.enabled(),
                config.strictMode(),
                config.defaultCaptureStackTraces(),
                config.defaultConventionCleanupDetection(),
                config.hasRetainedAfterCleanupDiagnosticHook(),
                LeakWatchWeavingState.aspectjRuntimePresent(),
                LeakWatchWeavingState.leakWatchAspectPresentOnClasspath(),
                LeakWatchWeavingState.aspectClassLoaded(),
                LeakWatchWeavingState.adviceObserved(),
                LeakWatchWeavingState.lastAdviceKind(),
                config.excludedPackages()
            );
        }
    }

    public void noteLeakTrackedClass(String className) {
        if (leakTrackedClasses.add(className)) {
            log.info("LeakWatch discovered @LeakTracked class: {}", className);
        }
    }

    public void noteRetentionClass(String className) {
        if (retentionTrackedClasses.add(className)) {
            log.info("LeakWatch discovered @RetentionSuspect class: {}", className);
        }
    }

    public void warnStrictOnce(String warningKey, String className, LeakReporter reporter, String message) {
        if (strictWarnings.add(warningKey)) {
            log.warn("LeakWatch strict-mode warning: {}", message);
            reporter.report(new LeakReport(
                LeakReportType.STRICT_MODE_WARNING,
                -1L,
                className,
                Set.of(),
                null,
                Instant.now(),
                Duration.ZERO,
                Set.of("strict-mode"),
                null,
                message,
                Severity.WARN
            ));
        }
    }

    public void warnRetentionSizingUnavailableOnce(String className, LeakWatchConfig config, LeakReporter reporter, String estimatorDescription) {
        String warningKey = "retention-sizing-unavailable:" + className;
        String message = "@RetentionSuspect byte budget for " + className
            + " is configured but approximate shallow-size estimation is unavailable. Byte-budget reporting is disabled for this type until a ShallowSizeEstimator is configured. estimator="
            + estimatorDescription;
        warnRetentionSizingOnce(warningKey, className, config, reporter, message);
    }

    public void warnRetentionSizingFailureOnce(String className, LeakWatchConfig config, LeakReporter reporter, String estimatorDescription, Throwable failure) {
        String warningKey = "retention-sizing-failure:" + className + ":" + failure.getClass().getName();
        String message = "@RetentionSuspect byte budget for " + className
            + " could not be evaluated because shallow-size estimation failed. Byte-budget reporting is disabled for this type until the estimator is fixed. estimator="
            + estimatorDescription + ", failure=" + failure.getClass().getName() + ": " + failure.getMessage();
        warnRetentionSizingOnce(warningKey, className, config, reporter, message);
    }

    private void warnRetentionSizingOnce(String warningKey, String className, LeakWatchConfig config, LeakReporter reporter, String message) {
        if (retentionSizingWarnings.add(warningKey)) {
            log.warn("LeakWatch retention sizing warning: {}", message);
            if (config.strictMode()) {
                reporter.report(new LeakReport(
                    LeakReportType.STRICT_MODE_WARNING,
                    -1L,
                    className,
                    Set.of(),
                    null,
                    Instant.now(),
                    Duration.ZERO,
                    Set.of("strict-mode", "retention"),
                    null,
                    message,
                    Severity.WARN
                ));
            }
        }
    }
}
