package cafe.woden.leakwatch.smoketest;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"smoke-test", "published-artifact"})
public final class PublishedAnnotatedResource {
    @CleanupMethod
    public void release() {
        // smoke-test cleanup marker
    }
}
