package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.FallbackCleanup;
import cafe.woden.leakwatch.annotations.FallbackCleanupStateProvider;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LeakTracked(tags = {"sample", "strict", "fallback", "action-instantiation"})
@FallbackCleanup(action = ConstructorBoundDeletePathFallbackCleanup.class)
public final class ConstructorBoundFallbackActionResource implements AutoCloseable, FallbackCleanupStateProvider<Path> {
    private final Path path;

    public ConstructorBoundFallbackActionResource(String prefix) throws IOException {
        this.path = Files.createTempFile(prefix, ".tmp");
        Files.writeString(path, "strict-sample");
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
