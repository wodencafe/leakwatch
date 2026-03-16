package cafe.woden.leakwatch.sample.jfr;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"sample", "jfr"})
public final class JfrSampleResource implements AutoCloseable {
    private final String name;

    public JfrSampleResource(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @CleanupMethod
    @Override
    public void close() {
        // explicit lifecycle boundary for sample coverage
    }
}
