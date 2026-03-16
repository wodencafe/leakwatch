package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = { "sample", "convention" })
public final class ConventionalCleanupResource {
    private final String name;

    public ConventionalCleanupResource(String name) {
        this.name = name;
    }

    public void shutdown() {
        System.out.println("Shutdown " + name);
    }
}
