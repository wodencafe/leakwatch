package cafe.woden.leakwatch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a type that should stay within a retention budget.
 *
 * <p>Use this for types that do not have a single cleanup call, but should not quietly pile up
 * over time. Typical examples are caches, registries, listeners, and session-like objects.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface RetentionSuspect {
    /**
     * Maximum live instance count before LeakWatch reports the type.
     *
     * @return max live instances, or {@code -1} to disable count-based reporting
     */
    long maxLiveInstances() default -1;

    /**
     * Maximum approximate shallow bytes before LeakWatch reports the type.
     *
     * @return max approximate shallow bytes, or {@code -1} to disable byte-based reporting
     */
    long maxApproxShallowBytes() default -1;

    /**
     * Whether to capture an allocation stack trace for retained instances.
     *
     * @return {@code true} to record where instances were created
     */
    boolean captureStackTrace() default false;

    /**
     * Severity attached to retention reports for this type.
     *
     * @return report severity
     */
    Severity severity() default Severity.WARN;

    /**
     * Extra tags to copy onto reports for this type.
     *
     * @return optional descriptive tags
     */
    String[] tags() default {};
}
