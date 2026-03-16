package cafe.woden.leakwatch.sample.observability;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = {"sample", "observability"})
public final class ObservableSampleResource {
    private final String name;

    public ObservableSampleResource(String name) {
        this.name = name;
    }

    @CleanupMethod
    public void close() {
        System.out.println("Closed observable resource " + name);
    }
}
