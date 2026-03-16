package cafe.woden.leakwatch.sample.closeable;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = { "sample", "annotated", "indirect" })
public final class IndirectAnnotatedCleanupResource {
    private final String name;

    public IndirectAnnotatedCleanupResource(String name) {
        this.name = name;
    }

    public void release() {
        cleanup();
    }

    @CleanupMethod
    private void cleanup() {
        System.out.println("Indirect cleanup " + name);
    }
}
