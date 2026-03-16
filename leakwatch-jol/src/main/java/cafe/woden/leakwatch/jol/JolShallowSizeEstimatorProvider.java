package cafe.woden.leakwatch.jol;

import cafe.woden.leakwatch.core.ShallowSizeEstimator;
import cafe.woden.leakwatch.core.ShallowSizeEstimatorProvider;
import org.openjdk.jol.vm.VM;

/**
 * {@link cafe.woden.leakwatch.core.ShallowSizeEstimatorProvider} backed by JOL.
 */
public final class JolShallowSizeEstimatorProvider implements ShallowSizeEstimatorProvider {
    @Override
    public String name() {
        return "leakwatch-jol";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public ShallowSizeEstimator createEstimator() {
        return new JolShallowSizeEstimator(VM.current());
    }
}
