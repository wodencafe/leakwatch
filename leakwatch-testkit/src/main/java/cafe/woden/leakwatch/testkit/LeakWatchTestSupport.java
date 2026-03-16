package cafe.woden.leakwatch.testkit;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimator;

/**
 * Convenience methods for wiring LeakWatch into tests.
 */
public final class LeakWatchTestSupport {
    private LeakWatchTestSupport() {
    }

    /**
     * Configures LeakWatch with defaults and returns an in-memory reporter for assertions.
     */
    public static InMemoryLeakReporter configureInMemory() {
        return configureInMemory(LeakWatchConfig.defaults());
    }

    /**
     * Configures LeakWatch with defaults and an explicit strict-mode flag.
     */
    public static InMemoryLeakReporter configureInMemory(boolean strictMode) {
        LeakWatchConfig defaults = LeakWatchConfig.defaults();
        return configureInMemory(new LeakWatchConfig(
            defaults.enabled(),
            defaults.defaultCaptureStackTraces(),
            strictMode,
            defaults.defaultConventionCleanupDetection(),
            defaults.excludedPackages()
        ));
    }

    /**
     * Configures LeakWatch with the supplied config and an in-memory reporter.
     */
    public static InMemoryLeakReporter configureInMemory(LeakWatchConfig config) {
        return configureInMemory(config, ShallowSizeEstimator.unsupported());
    }

    /**
     * Configures LeakWatch with the supplied config, in-memory reporter, and shallow-size estimator.
     */
    public static InMemoryLeakReporter configureInMemory(LeakWatchConfig config, ShallowSizeEstimator shallowSizeEstimator) {
        InMemoryLeakReporter reporter = new InMemoryLeakReporter();
        LeakWatchRuntime.configure(config, reporter, shallowSizeEstimator);
        return reporter;
    }
}
