package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakWatchSystemPropertiesTest {
    @Test
    void loadWithoutOverridesMatchesDefaults() {
        LeakWatchConfig config = LeakWatchSystemProperties.load(new Properties());

        assertEquals(LeakWatchConfig.defaults(), config);
    }

    @Test
    void loadAppliesBooleanAndExcludedPackageOverrides() {
        Properties properties = new Properties();
        properties.setProperty(LeakWatchSystemProperties.ENABLED, "false");
        properties.setProperty(LeakWatchSystemProperties.CAPTURE_STACK_TRACES, "true");
        properties.setProperty(LeakWatchSystemProperties.STRICT_MODE, "true");
        properties.setProperty(LeakWatchSystemProperties.CONVENTION_CLEANUP_DETECTION, "false");
        properties.setProperty(LeakWatchSystemProperties.ADDITIONAL_EXCLUDED_PACKAGES, "com.example.generated, com.example.legacy. , ,com.example.thirdparty");

        LeakWatchConfig config = LeakWatchSystemProperties.load(properties);

        assertFalse(config.enabled());
        assertTrue(config.defaultCaptureStackTraces());
        assertTrue(config.strictMode());
        assertFalse(config.defaultConventionCleanupDetection());
        assertTrue(config.excludedPackages().containsAll(Set.of(
            "com.example.generated",
            "com.example.legacy.",
            "com.example.thirdparty"
        )));
        assertTrue(config.excludedPackages().contains("cafe.woden.leakwatch.core."));
    }

    @Test
    void environmentVariablesCanPopulateConfiguration() {
        LeakWatchConfig config = LeakWatchSystemProperties.load(
            new Properties(),
            Map.of(
                "LEAKWATCH_ENABLED", "false",
                "LEAKWATCH_CAPTURE_STACK_TRACES", "true",
                "LEAKWATCH_STRICT_MODE", "true",
                "LEAKWATCH_CONVENTION_CLEANUP_DETECTION", "false",
                "LEAKWATCH_ADDITIONAL_EXCLUDED_PACKAGES", "com.example.env,com.example.shared"
            )
        );

        assertFalse(config.enabled());
        assertTrue(config.defaultCaptureStackTraces());
        assertTrue(config.strictMode());
        assertFalse(config.defaultConventionCleanupDetection());
        assertTrue(config.excludedPackages().containsAll(Set.of("com.example.env", "com.example.shared")));
    }

    @Test
    void systemPropertiesOverrideEnvironmentVariables() {
        Properties properties = new Properties();
        properties.setProperty(LeakWatchSystemProperties.ENABLED, "true");
        properties.setProperty(LeakWatchSystemProperties.STRICT_MODE, "false");

        LeakWatchConfig config = LeakWatchSystemProperties.load(
            properties,
            Map.of(
                "LEAKWATCH_ENABLED", "false",
                "LEAKWATCH_STRICT_MODE", "true"
            )
        );

        assertTrue(config.enabled());
        assertFalse(config.strictMode());
    }

    @Test
    void invalidBooleanValuesFallBackToDefaults() {
        Properties properties = new Properties();
        properties.setProperty(LeakWatchSystemProperties.ENABLED, "definitely");
        properties.setProperty(LeakWatchSystemProperties.STRICT_MODE, "sometimes");

        LeakWatchConfig config = LeakWatchSystemProperties.load(properties);

        assertEquals(LeakWatchConfig.defaults().enabled(), config.enabled());
        assertEquals(LeakWatchConfig.defaults().strictMode(), config.strictMode());
    }
}
