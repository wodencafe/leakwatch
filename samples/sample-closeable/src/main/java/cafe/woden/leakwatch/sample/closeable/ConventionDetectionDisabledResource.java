package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(conventionCleanupDetection = false, tags = { "sample", "misconfigured", "convention-disabled" })
public final class ConventionDetectionDisabledResource {
    private final String name;

    public ConventionDetectionDisabledResource(String name) {
        this.name = name;
    }

    public void shutdown() {
        System.out.println("Shutdown " + name);
    }
}
