package cafe.woden.leakwatch.sample.rolling;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"rolling", "json"})
final class RollingJsonSampleResource implements AutoCloseable {
    private final String name;
    private boolean closed;

    RollingJsonSampleResource(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    boolean closed() {
        return closed;
    }

    @CleanupMethod
    @Override
    public void close() {
        closed = true;
    }
}
