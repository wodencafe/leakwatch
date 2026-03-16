package cafe.woden.leakwatch.sample.micrometer;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = {"sample", "micrometer"})
public final class MeteredSampleResource {
    private final String name;

    public MeteredSampleResource(String name) {
        this.name = name;
    }

    @CleanupMethod
    public void close() {
        System.out.println("Closed metered resource " + name);
    }
}
