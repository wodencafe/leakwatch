package cafe.woden.leakwatch.core;

import java.util.Set;

/**
 * Built-in cleanup method names that LeakWatch treats as conventional lifecycle methods.
 */
public final class CleanupConventions {
    public static final Set<String> COMMON_METHOD_NAMES = Set.of(
        "close",
        "dispose",
        "shutdown",
        "disconnect",
        "unsubscribe"
    );

    private CleanupConventions() {
    }

    public static boolean isCommonCleanupMethodName(String methodName) {
        return COMMON_METHOD_NAMES.contains(methodName);
    }
}
