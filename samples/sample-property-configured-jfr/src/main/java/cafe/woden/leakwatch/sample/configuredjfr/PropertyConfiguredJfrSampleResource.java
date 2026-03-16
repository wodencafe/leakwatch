package cafe.woden.leakwatch.sample.configuredjfr;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"sample", "configured-jfr"})
public final class PropertyConfiguredJfrSampleResource implements AutoCloseable {
    private final String name;

    public PropertyConfiguredJfrSampleResource(String name) {
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
