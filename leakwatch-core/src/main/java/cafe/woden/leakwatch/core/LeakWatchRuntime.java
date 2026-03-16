package cafe.woden.leakwatch.core;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global access point for the active LeakWatch registry.
 * <p>
 * Applications usually configure LeakWatch once during startup and then let woven advice use this runtime.
 */
public final class LeakWatchRuntime {
    private static final AtomicReference<LeakRegistry> REGISTRY =
        new AtomicReference<>(new LeakRegistry(LeakWatchConfig.defaults(), new Slf4jLeakReporter(), ShallowSizeEstimators.autoDiscovering()));

    private LeakWatchRuntime() {
    }

    /**
     * Returns the currently active registry.
     */
    public static LeakRegistry registry() {
        return REGISTRY.get();
    }

    /**
     * Replaces the active registry with the supplied configuration and reporter.
     */
    public static void configure(LeakWatchConfig config, LeakReporter reporter) {
        configure(config, reporter, ShallowSizeEstimators.autoDiscovering());
    }

    /**
     * Replaces the active registry and also supplies the shallow-size estimator to use for retention budgets.
     */
    public static void configure(LeakWatchConfig config, LeakReporter reporter, ShallowSizeEstimator shallowSizeEstimator) {
        LeakRegistry replacement = new LeakRegistry(
            Objects.requireNonNull(config, "config"),
            Objects.requireNonNull(reporter, "reporter"),
            Objects.requireNonNull(shallowSizeEstimator, "shallowSizeEstimator")
        );
        LeakRegistry previous = REGISTRY.getAndSet(replacement);
        if (previous != null) {
            previous.close();
        }
    }

    /**
     * Loads core configuration from system properties and uses the supplied reporter.
     */
    public static void configureFromSystemProperties(LeakReporter reporter) {
        configure(LeakWatchSystemProperties.load(), reporter);
    }

    /**
     * Loads both configuration and reporters from system properties.
     */
    public static void configureFromSystemProperties() {
        configure(LeakWatchSystemProperties.load(), LeakWatchReporterSystemProperties.load());
    }

    /**
     * Loads core configuration from environment variables and uses the supplied reporter.
     */
    public static void configureFromEnvironment(LeakReporter reporter) {
        configure(LeakWatchSystemProperties.load(new Properties(), System.getenv()), reporter);
    }

    /**
     * Loads both configuration and reporters from environment variables.
     */
    public static void configureFromEnvironment() {
        configure(
            LeakWatchSystemProperties.load(new Properties(), System.getenv()),
            LeakWatchReporterSystemProperties.load(new Properties(), System.getenv())
        );
    }

    /**
     * Loads configuration from system properties first, then environment variables, and uses the supplied reporter.
     */
    public static void configureFromSystemPropertiesAndEnvironment(LeakReporter reporter) {
        configure(LeakWatchSystemProperties.load(System.getProperties(), System.getenv()), reporter);
    }

    /**
     * Loads both configuration and reporters from system properties first, then environment variables.
     */
    public static void configureFromSystemPropertiesAndEnvironment() {
        configure(
            LeakWatchSystemProperties.load(System.getProperties(), System.getenv()),
            LeakWatchReporterSystemProperties.load(System.getProperties(), System.getenv())
        );
    }

    static void configureForTesting(Properties properties, Map<String, String> environment, LeakReporter reporter) {
        configure(LeakWatchSystemProperties.load(properties, environment), reporter);
    }

    static void configureForTesting(Properties properties, Map<String, String> environment) {
        configure(
            LeakWatchSystemProperties.load(properties, environment),
            LeakWatchReporterSystemProperties.load(properties, environment)
        );
    }
}
