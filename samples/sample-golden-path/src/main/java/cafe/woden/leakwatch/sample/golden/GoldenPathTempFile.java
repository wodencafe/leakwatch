package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.FallbackCleanup;
import cafe.woden.leakwatch.annotations.FallbackCleanupStateProvider;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LeakTracked(captureStackTrace = true, tags = {"sample", "golden-path", "fallback"})
@FallbackCleanup(action = DeletePathFallbackCleanup.class)
public final class GoldenPathTempFile implements AutoCloseable, FallbackCleanupStateProvider<Path> {
    private final Path path;

    public GoldenPathTempFile(String prefix) throws IOException {
        this.path = Files.createTempFile(prefix, ".tmp");
        Files.writeString(path, "golden-path temp content");
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
