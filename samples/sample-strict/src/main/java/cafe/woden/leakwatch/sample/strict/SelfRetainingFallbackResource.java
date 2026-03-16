package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.FallbackCleanup;
import cafe.woden.leakwatch.annotations.FallbackCleanupStateProvider;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LeakTracked(tags = {"sample", "strict", "fallback", "self-retaining"})
@FallbackCleanup(action = DeletePathFallbackCleanup.class)
public final class SelfRetainingFallbackResource implements AutoCloseable, FallbackCleanupStateProvider<Object> {
    private final Path path;

    public SelfRetainingFallbackResource(String prefix) throws IOException {
        this.path = Files.createTempFile(prefix, ".tmp");
        Files.writeString(path, "strict-sample");
    }

    public Path path() {
        return path;
    }

    @Override
    public Object leakWatchFallbackCleanupState() {
        return this;
    }

    @Override
    @CleanupMethod
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
