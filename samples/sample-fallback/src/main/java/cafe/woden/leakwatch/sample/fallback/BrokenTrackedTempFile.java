package cafe.woden.leakwatch.sample.fallback;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.FallbackCleanup;
import cafe.woden.leakwatch.annotations.FallbackCleanupStateProvider;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LeakTracked(tags = {"temp-file", "fallback-cleanup", "broken-fallback"})
@FallbackCleanup(action = ExplodingPathFallbackCleanup.class)
public final class BrokenTrackedTempFile implements AutoCloseable, FallbackCleanupStateProvider<Path> {
    private final Path path;

    public BrokenTrackedTempFile(String prefix) throws IOException {
        this.path = Files.createTempFile(prefix, ".tmp");
        Files.writeString(path, "temporary leakwatch content");
    }

    public Path path() {
        return path;
    }

    @Override
    public Path leakWatchFallbackCleanupState() {
        return path;
    }

    @Override
    @CleanupMethod
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
