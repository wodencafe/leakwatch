package cafe.woden.leakwatch.core;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Loads core LeakWatch configuration from system properties and environment variables.
 */
public final class LeakWatchSystemProperties {
    public static final String ENABLED = "leakwatch.enabled";
    public static final String CAPTURE_STACK_TRACES = "leakwatch.captureStackTraces";
    public static final String STRICT_MODE = "leakwatch.strictMode";
    public static final String CONVENTION_CLEANUP_DETECTION = "leakwatch.conventionCleanupDetection";
    public static final String ADDITIONAL_EXCLUDED_PACKAGES = "leakwatch.additionalExcludedPackages";

    private LeakWatchSystemProperties() {
    }

    /**
     * Loads configuration from the current JVM system properties.
     */
    public static LeakWatchConfig load() {
        return load(System.getProperties());
    }

    /**
     * Loads configuration from the supplied properties object.
     */
    public static LeakWatchConfig load(Properties properties) {
        return load(properties, Map.of());
    }

    /**
     * Loads configuration using system properties first and environment variables as a fallback.
     */
    public static LeakWatchConfig load(Properties properties, Map<String, String> environment) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(environment, "environment");

        LeakWatchConfig defaults = LeakWatchConfig.defaults();
        return new LeakWatchConfig(
            parseBoolean(properties, environment, ENABLED, defaults.enabled()),
            parseBoolean(properties, environment, CAPTURE_STACK_TRACES, defaults.defaultCaptureStackTraces()),
            parseBoolean(properties, environment, STRICT_MODE, defaults.strictMode()),
            parseBoolean(properties, environment, CONVENTION_CLEANUP_DETECTION, defaults.defaultConventionCleanupDetection()),
            mergeExcludedPackages(defaults.excludedPackages(), LeakWatchConfigurationSources.resolve(
                properties,
                environment,
                ADDITIONAL_EXCLUDED_PACKAGES
            )),
            defaults.retainedAfterCleanupDiagnosticHook()
        );
    }

    private static boolean parseBoolean(
        Properties properties,
        Map<String, String> environment,
        String propertyName,
        boolean defaultValue
    ) {
        String raw = LeakWatchConfigurationSources.resolve(properties, environment, propertyName);
        if (raw == null) {
            return defaultValue;
        }

        String normalized = raw.trim();
        if (normalized.equalsIgnoreCase("true")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("false")) {
            return false;
        }
        return defaultValue;
    }

    private static Set<String> mergeExcludedPackages(Set<String> defaults, String rawAdditionalPackages) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(defaults);
        if (rawAdditionalPackages == null || rawAdditionalPackages.isBlank()) {
            return Set.copyOf(merged);
        }

        for (String token : rawAdditionalPackages.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                merged.add(trimmed);
            }
        }
        return Set.copyOf(merged);
    }
}
