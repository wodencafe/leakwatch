package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.CleanupMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reflective helper that inspects a class once and records which cleanup methods LeakWatch should recognize.
 */
public final class ClassIntrospector {
    private ClassIntrospector() {
    }

    public static CleanupMetadata inspect(Class<?> type) {
        Set<String> explicitCleanupMethods = new LinkedHashSet<>();
        Set<String> conventionalCleanupMethods = new LinkedHashSet<>();

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                if (method.isSynthetic()) {
                    continue;
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.isAnnotationPresent(CleanupMethod.class)) {
                    explicitCleanupMethods.add(method.getName());
                }
                if (CleanupConventions.isCommonCleanupMethodName(method.getName())) {
                    conventionalCleanupMethods.add(method.getName());
                }
            }
        }

        return new CleanupMetadata(
            type.getName(),
            Set.copyOf(explicitCleanupMethods),
            Set.copyOf(conventionalCleanupMethods)
        );
    }
}
