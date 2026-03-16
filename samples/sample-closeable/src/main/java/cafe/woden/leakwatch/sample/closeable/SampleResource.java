package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = { "sample", "resource" })
public final class SampleResource {
    private final String name;

    public SampleResource(String name) {
        this.name = name;
    }

    @CleanupMethod
    public void close() {
        System.out.println("Closed " + name);
    }
}
