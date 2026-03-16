package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.FallbackCleanup;
import cafe.woden.leakwatch.annotations.FallbackCleanupAction;
import cafe.woden.leakwatch.annotations.FallbackCleanupStateProvider;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

final class FallbackCleanupSupport {
    private FallbackCleanupSupport() {
    }

    static FallbackCleanupPlan resolve(
        Object instance,
        Class<?> type,
        LeakWatchConfig config,
        LeakReporter reporter,
        StartupDiagnostics diagnostics
    ) {
        FallbackCleanup annotation = type.getAnnotation(FallbackCleanup.class);
        if (annotation == null) {
            return null;
        }

        if (!(instance instanceof FallbackCleanupStateProvider<?> provider)) {
            warnStrict(
                config,
                reporter,
                diagnostics,
                "fallback-state-provider:" + type.getName(),
                type,
                "@FallbackCleanup on " + type.getName() + " was ignored because the class does not implement FallbackCleanupStateProvider."
            );
            return null;
        }

        Object detachedState;
        try {
            detachedState = provider.leakWatchFallbackCleanupState();
        } catch (RuntimeException ex) {
            warnStrict(
                config,
                reporter,
                diagnostics,
                "fallback-state-capture:" + type.getName(),
                type,
                "@FallbackCleanup on " + type.getName() + " was ignored because leakWatchFallbackCleanupState() threw " + ex.getClass().getName() + "."
            );
            return null;
        }

        if (detachedState == instance) {
            warnStrict(
                config,
                reporter,
                diagnostics,
                "fallback-self-retain:" + type.getName(),
                type,
                "@FallbackCleanup on " + type.getName() + " was ignored because leakWatchFallbackCleanupState() returned the tracked instance itself. Detached state must not strongly retain the referent."
            );
            return null;
        }

        FallbackCleanupAction<Object> action = instantiate(annotation.action(), config, reporter, diagnostics, type);
        if (action == null) {
            return null;
        }

        return new FallbackCleanupPlan(action, detachedState, annotation.action().getName());
    }

    @SuppressWarnings("unchecked")
    private static FallbackCleanupAction<Object> instantiate(
        Class<? extends FallbackCleanupAction<?>> actionType,
        LeakWatchConfig config,
        LeakReporter reporter,
        StartupDiagnostics diagnostics,
        Class<?> ownerType
    ) {
        try {
            Constructor<? extends FallbackCleanupAction<?>> constructor = actionType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (FallbackCleanupAction<Object>) constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            warnStrict(
                config,
                reporter,
                diagnostics,
                "fallback-action-instantiation:" + ownerType.getName(),
                ownerType,
                "@FallbackCleanup on " + ownerType.getName() + " was ignored because action " + actionType.getName() + " could not be instantiated with a no-arg constructor."
            );
            return null;
        }
    }

    private static void warnStrict(
        LeakWatchConfig config,
        LeakReporter reporter,
        StartupDiagnostics diagnostics,
        String warningKey,
        Class<?> type,
        String message
    ) {
        if (!config.strictMode()) {
            return;
        }

        diagnostics.warnStrictOnce(warningKey, type.getName(), reporter, message);
    }
}
