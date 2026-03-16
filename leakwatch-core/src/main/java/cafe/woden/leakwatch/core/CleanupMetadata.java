package cafe.woden.leakwatch.core;

import java.util.Set;

/**
 * Cached cleanup metadata for a tracked type.
 * <p>
 * This keeps explicit {@code @CleanupMethod} names separate from methods found by convention.
 */
public record CleanupMetadata(
    String className,
    Set<String> explicitCleanupMethods,
    Set<String> conventionalCleanupMethods
) {
    public boolean hasExplicitCleanupMethod() {
        return !explicitCleanupMethods.isEmpty();
    }

    public boolean hasAnyCleanupMethod() {
        return hasExplicitCleanupMethod() || !conventionalCleanupMethods.isEmpty();
    }

    public boolean hasConventionalMethod(String methodName) {
        return conventionalCleanupMethods.contains(methodName);
    }
}
