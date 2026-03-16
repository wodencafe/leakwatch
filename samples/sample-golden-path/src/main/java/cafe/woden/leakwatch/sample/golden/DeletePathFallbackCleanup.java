package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.FallbackCleanupAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeletePathFallbackCleanup implements FallbackCleanupAction<Path> {
    @Override
    public void cleanup(Path detachedState) throws IOException {
        if (detachedState != null) {
            Files.deleteIfExists(detachedState);
        }
    }
}
