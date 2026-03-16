package cafe.woden.leakwatch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a type that is expected to hit an explicit cleanup step.
 *
 * <p>Use this for things with a real end-of-life method, like {@code close()}, {@code dispose()},
 * or {@code shutdown()}. LeakWatch uses that cleanup boundary to decide whether the object was
 * cleaned up before it became unreachable.
 */
@Documented
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface LeakTracked {
    /**
     * Whether to capture an allocation stack trace for instances of this type.
     *
     * @return {@code true} to record where the object was created
     */
    boolean captureStackTrace() default false;

    /**
     * Whether LeakWatch may use conventional cleanup names such as {@code close()} or
     * {@code dispose()} when no {@link CleanupMethod} is present.
     *
     * @return {@code true} to allow conventional cleanup name detection
     */
    boolean conventionCleanupDetection() default true;

    /**
     * Extra tags to copy onto reports for this type.
     *
     * @return optional descriptive tags
     */
    String[] tags() default {};
}
