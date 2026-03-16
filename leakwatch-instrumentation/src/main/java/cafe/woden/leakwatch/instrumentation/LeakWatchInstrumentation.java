package cafe.woden.leakwatch.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.Objects;

/**
 * Holder for the active {@link java.lang.instrument.Instrumentation} instance installed by the LeakWatch agent.
 */
public final class LeakWatchInstrumentation {
    private static volatile Instrumentation instrumentation;

    private LeakWatchInstrumentation() {
    }

    static void install(Instrumentation instrumentation) {
        LeakWatchInstrumentation.instrumentation = Objects.requireNonNull(instrumentation, "instrumentation");
    }

    /**
     * Returns the active instrumentation instance, or {@code null} when the agent is not installed.
     */
    public static Instrumentation instrumentation() {
        return instrumentation;
    }

    /**
     * Returns whether the LeakWatch instrumentation agent is currently installed.
     */
    public static boolean isAvailable() {
        return instrumentation != null;
    }

    static void clear() {
        instrumentation = null;
    }
}
