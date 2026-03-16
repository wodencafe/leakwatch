package cafe.woden.leakwatch.core;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

final class LeakWatchConfigurationSources {
    private LeakWatchConfigurationSources() {
    }

    static String resolve(Properties properties, Map<String, String> environment, String propertyName) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(propertyName, "propertyName");

        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null) {
            return propertyValue;
        }

        return environment.get(environmentVariableNameFor(propertyName));
    }

    static String environmentVariableNameFor(String propertyName) {
        Objects.requireNonNull(propertyName, "propertyName");
        return propertyName
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .replace('.', '_')
            .replace('-', '_')
            .toUpperCase(Locale.ROOT);
    }
}
