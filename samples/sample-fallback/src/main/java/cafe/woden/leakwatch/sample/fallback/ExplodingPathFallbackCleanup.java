package cafe.woden.leakwatch.sample.fallback;

import cafe.woden.leakwatch.annotations.FallbackCleanupAction;

import java.io.IOException;
import java.nio.file.Path;

public final class ExplodingPathFallbackCleanup implements FallbackCleanupAction<Path> {
    @Override
    public void cleanup(Path detachedState) throws IOException {
        throw new IOException("simulated fallback failure for " + detachedState);
    }
}
