package cafe.woden.leakwatch.smoketest;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(tags = {"smoke-test", "published-artifact"})
public final class PublishedConventionalResource {
    public void close() {
        // smoke-test conventional cleanup marker
    }
}
