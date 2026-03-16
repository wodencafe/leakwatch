package cafe.woden.leakwatch.core;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Loads reporter wiring from system properties and environment variables.
 */
public final class LeakWatchReporterSystemProperties {
    public static final String REPORTERS = "leakwatch.reporters";
    public static final String JSON_PATH = "leakwatch.json.path";
    public static final String ROLLING_JSON_PATH = "leakwatch.rollingJson.path";
    public static final String ROLLING_JSON_MAX_BYTES = "leakwatch.rollingJson.maxBytes";
    public static final String ROLLING_JSON_MAX_ARCHIVES = "leakwatch.rollingJson.maxArchives";

    private static final long DEFAULT_ROLLING_JSON_MAX_BYTES = 256L * 1024L;
    private static final int DEFAULT_ROLLING_JSON_MAX_ARCHIVES = 5;

    private LeakWatchReporterSystemProperties() {
    }

    /**
     * Loads reporters from the current JVM system properties.
     */
    public static LeakReporter load() {
        return load(System.getProperties());
    }

    /**
     * Loads reporters from the supplied properties object.
     */
    public static LeakReporter load(Properties properties) {
        return load(properties, Map.of());
    }

    /**
     * Loads reporters using system properties first and environment variables as a fallback.
     */
    public static LeakReporter load(Properties properties, Map<String, String> environment) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(environment, "environment");

        Set<String> reporterIds = parseReporterIds(LeakWatchConfigurationSources.resolve(properties, environment, REPORTERS));
        List<LeakReporter> reporters = new ArrayList<>();
        for (String reporterId : reporterIds) {
            reporters.add(createReporter(reporterId, properties, environment));
        }

        if (reporters.size() == 1) {
            return reporters.get(0);
        }
        return new CompositeLeakReporter(reporters);
    }

    private static Set<String> parseReporterIds(String rawReporterIds) {
        LinkedHashSet<String> reporterIds = new LinkedHashSet<>();
        if (rawReporterIds == null || rawReporterIds.isBlank()) {
            reporterIds.add("slf4j");
            return Collections.unmodifiableSet(new LinkedHashSet<>(reporterIds));
        }

        for (String token : rawReporterIds.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                reporterIds.add(normalized);
            }
        }

        if (reporterIds.isEmpty()) {
            reporterIds.add("slf4j");
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(reporterIds));
    }

    private static LeakReporter createReporter(String reporterId, Properties properties, Map<String, String> environment) {
        return switch (reporterId) {
            case "slf4j" -> new Slf4jLeakReporter();
            case "json", "json-file", "ndjson" -> new LeakReportJsonFileReporter(
                Path.of(requireNonBlank(properties, environment, JSON_PATH, reporterId))
            );
            case "rolling-json", "rolling_json", "rollingjson" -> new RollingLeakReportJsonFileReporter(
                Path.of(requireNonBlank(properties, environment, ROLLING_JSON_PATH, reporterId)),
                parsePositiveLong(properties, environment, ROLLING_JSON_MAX_BYTES, DEFAULT_ROLLING_JSON_MAX_BYTES),
                parseNonNegativeInt(properties, environment, ROLLING_JSON_MAX_ARCHIVES, DEFAULT_ROLLING_JSON_MAX_ARCHIVES)
            );
            case "jfr" -> instantiateOptionalReporter("jfr", "cafe.woden.leakwatch.jfr.JfrLeakReporter");
            default -> throw new IllegalArgumentException(
                "Unsupported LeakWatch reporter '" + reporterId + "'. Supported values: slf4j, json, rolling-json, jfr."
            );
        };
    }

    private static LeakReporter instantiateOptionalReporter(String reporterId, String className) {
        try {
            Class<?> reporterClass = Class.forName(className);
            if (!LeakReporter.class.isAssignableFrom(reporterClass)) {
                throw new IllegalArgumentException(
                    "LeakWatch reporter class '" + className + "' does not implement LeakReporter."
                );
            }

            return (LeakReporter) reporterClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException(
                "LeakWatch reporter '" + reporterId + "' requires optional module class '" + className + "' on the classpath.",
                exception
            );
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalArgumentException(
                "LeakWatch reporter '" + reporterId + "' could not be instantiated from class '" + className + "'.",
                exception
            );
        }
    }

    private static String requireNonBlank(
        Properties properties,
        Map<String, String> environment,
        String propertyName,
        String reporterId
    ) {
        String raw = LeakWatchConfigurationSources.resolve(properties, environment, propertyName);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                "Missing required configuration '" + propertyName + "' (env "
                    + LeakWatchConfigurationSources.environmentVariableNameFor(propertyName)
                    + ") for LeakWatch reporter '" + reporterId + "'."
            );
        }
        return raw.trim();
    }

    private static long parsePositiveLong(
        Properties properties,
        Map<String, String> environment,
        String propertyName,
        long defaultValue
    ) {
        String raw = LeakWatchConfigurationSources.resolve(properties, environment, propertyName);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            long parsed = Long.parseLong(raw.trim());
            if (parsed <= 0L) {
                throw new IllegalArgumentException(
                    "Configuration '" + propertyName + "' (env "
                        + LeakWatchConfigurationSources.environmentVariableNameFor(propertyName)
                        + ") must be greater than zero, but was "
                        + parsed
                        + "."
                );
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Configuration '" + propertyName + "' (env "
                    + LeakWatchConfigurationSources.environmentVariableNameFor(propertyName)
                    + ") must be a positive long, but was '"
                    + raw
                    + "'.",
                exception
            );
        }
    }

    private static int parseNonNegativeInt(
        Properties properties,
        Map<String, String> environment,
        String propertyName,
        int defaultValue
    ) {
        String raw = LeakWatchConfigurationSources.resolve(properties, environment, propertyName);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 0) {
                throw new IllegalArgumentException(
                    "Configuration '" + propertyName + "' (env "
                        + LeakWatchConfigurationSources.environmentVariableNameFor(propertyName)
                        + ") must be zero or greater, but was "
                        + parsed
                        + "."
                );
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Configuration '" + propertyName + "' (env "
                    + LeakWatchConfigurationSources.environmentVariableNameFor(propertyName)
                    + ") must be a non-negative integer, but was '"
                    + raw
                    + "'.",
                exception
            );
        }
    }
}
