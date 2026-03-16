package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.ExpectUnreachableAfterCleanup;
import cafe.woden.leakwatch.annotations.LeakTracked;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolves LeakWatch annotations, including meta-annotations.
 */
public final class TrackingAnnotationSupport {
    private TrackingAnnotationSupport() {
    }

    public static LeakTracked resolveLeakTracked(Class<?> type) {
        return resolve(type, LeakTracked.class);
    }

    public static ExpectUnreachableAfterCleanup resolveExpectUnreachableAfterCleanup(Class<?> type) {
        return resolve(type, ExpectUnreachableAfterCleanup.class);
    }

    private static <A extends Annotation> A resolve(Class<?> type, Class<A> annotationType) {
        A direct = type.getAnnotation(annotationType);
        if (direct != null) {
            return direct;
        }

        for (Annotation annotation : type.getAnnotations()) {
            A resolved = resolveFromAnnotationType(annotation.annotationType(), annotationType, new HashSet<>());
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private static <A extends Annotation> A resolveFromAnnotationType(
        Class<? extends Annotation> candidate,
        Class<A> target,
        Set<Class<?>> visited
    ) {
        if (!visited.add(candidate)) {
            return null;
        }

        A direct = candidate.getAnnotation(target);
        if (direct != null) {
            return direct;
        }

        for (Annotation nested : candidate.getAnnotations()) {
            A resolved = resolveFromAnnotationType(nested.annotationType(), target, visited);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }
}
