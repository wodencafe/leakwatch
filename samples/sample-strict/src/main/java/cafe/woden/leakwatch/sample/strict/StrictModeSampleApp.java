package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;

public final class StrictModeSampleApp {
    private StrictModeSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        LeakWatchConfig defaults = LeakWatchConfig.defaults();
        LeakWatchRuntime.configure(
            new LeakWatchConfig(
                defaults.enabled(),
                defaults.defaultCaptureStackTraces(),
                true,
                defaults.defaultConventionCleanupDetection(),
                defaults.excludedPackages()
            ),
            new Slf4jLeakReporter()
        );

        new NoCleanupTrackedResource("missing-cleanup");

        try (MissingFallbackStateProviderResource missingProvider = new MissingFallbackStateProviderResource("strict-missing-provider");
             SelfRetainingFallbackResource selfRetaining = new SelfRetainingFallbackResource("strict-self-retaining");
             ExplodingFallbackStateResource explodingState = new ExplodingFallbackStateResource("strict-state-throws");
             ConstructorBoundFallbackActionResource constructorBound = new ConstructorBoundFallbackActionResource("strict-action-instantiation")) {
            System.out.println("Constructed strict-mode sample resources.");
        }

        System.out.println("Strict-mode sample complete.");
    }
}
