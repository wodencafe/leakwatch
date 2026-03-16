package cafe.woden.leakwatch.instrumentation;

import cafe.woden.leakwatch.core.ShallowSizeEstimator;
import cafe.woden.leakwatch.core.ShallowSizeEstimatorProvider;

/**
 * {@link cafe.woden.leakwatch.core.ShallowSizeEstimatorProvider} backed by the LeakWatch instrumentation agent.
 */
public final class InstrumentationShallowSizeEstimatorProvider implements ShallowSizeEstimatorProvider {
    @Override
    public String name() {
        return "leakwatch-instrumentation";
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public ShallowSizeEstimator createEstimator() {
        return new InstrumentationShallowSizeEstimator();
    }
}
