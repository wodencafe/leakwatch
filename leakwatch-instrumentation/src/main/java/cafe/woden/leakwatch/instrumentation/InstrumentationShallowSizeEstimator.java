package cafe.woden.leakwatch.instrumentation;

import cafe.woden.leakwatch.core.ShallowSizeEstimator;

import java.lang.instrument.Instrumentation;

/**
 * {@link cafe.woden.leakwatch.core.ShallowSizeEstimator} that uses a Java instrumentation agent.
 */
public final class InstrumentationShallowSizeEstimator implements ShallowSizeEstimator {
    @Override
    public long estimateShallowSize(Object instance) {
        Instrumentation instrumentation = LeakWatchInstrumentation.instrumentation();
        if (instrumentation == null) {
            throw new IllegalStateException("LeakWatch instrumentation agent is not installed");
        }
        return instrumentation.getObjectSize(instance);
    }

    @Override
    public boolean isAvailable() {
        return LeakWatchInstrumentation.isAvailable();
    }

    @Override
    public String description() {
        return "java.lang.instrument.Instrumentation#getObjectSize via leakwatch-instrumentation agent";
    }
}
