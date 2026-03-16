package cafe.woden.leakwatch.core;

/**
 * Service-provider hook used to supply a {@link ShallowSizeEstimator}.
 */
public interface ShallowSizeEstimatorProvider {
    /**
     * Stable provider name used for diagnostics and tie-breaking.
     */
    String name();

    /**
     * Provider priority. Higher values win during auto-discovery.
     */
    default int priority() {
        return 0;
    }

    /**
     * Creates the estimator for this provider.
     */
    ShallowSizeEstimator createEstimator();
}
