package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = { "sample", "misconfigured" })
public final class MisconfiguredTrackedResource {
    private final String name;

    public MisconfiguredTrackedResource(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
