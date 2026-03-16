package cafe.woden.leakwatch.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Best-effort runtime view of whether LeakWatch's AspectJ integration appears to be present and active.
 * <p>
 * This intentionally avoids a hard dependency from {@code leakwatch-core} back to AspectJ types. The core module can
 * log what it knows, while the aspect module can opt in by calling the marker methods below.
 */
public final class LeakWatchWeavingState {
    private static final AtomicBoolean ASPECT_CLASS_LOADED = new AtomicBoolean(false);
    private static final AtomicBoolean ADVICE_OBSERVED = new AtomicBoolean(false);
    private static final AtomicReference<String> LAST_ADVICE_KIND = new AtomicReference<>();

    private LeakWatchWeavingState() {
    }

    public static void markAspectClassLoaded() {
        ASPECT_CLASS_LOADED.set(true);
    }

    public static void markAdviceObserved(String adviceKind) {
        ASPECT_CLASS_LOADED.set(true);
        ADVICE_OBSERVED.set(true);
        LAST_ADVICE_KIND.set(adviceKind);
    }

    public static boolean aspectjRuntimePresent() {
        return isClassPresent("org.aspectj.lang.JoinPoint");
    }

    public static boolean leakWatchAspectPresentOnClasspath() {
        return isClassPresent("cafe.woden.leakwatch.aspectj.LeakTrackingAspect");
    }

    public static boolean aspectClassLoaded() {
        return ASPECT_CLASS_LOADED.get();
    }

    public static boolean adviceObserved() {
        return ADVICE_OBSERVED.get();
    }

    public static String lastAdviceKind() {
        return LAST_ADVICE_KIND.get();
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, LeakWatchWeavingState.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
