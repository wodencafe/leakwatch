package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.annotations.FallbackCleanupAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConstructorBoundDeletePathFallbackCleanup implements FallbackCleanupAction<Path> {
    private final String label;

    public ConstructorBoundDeletePathFallbackCleanup(String label) {
        this.label = label;
    }

    @Override
    public void cleanup(Path detachedState) throws IOException {
        if (detachedState != null && !label.isBlank()) {
            Files.deleteIfExists(detachedState);
        }
    }
}
