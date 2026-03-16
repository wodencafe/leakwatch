package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"sample", "strict", "missing-cleanup"})
public final class NoCleanupTrackedResource {
    private final String name;

    public NoCleanupTrackedResource(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
