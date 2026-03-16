package cafe.woden.leakwatch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Says that instances should become unreachable soon after cleanup.
 *
 * <p>Use this with {@link LeakTracked} when cleanup is not the whole story and you also want
 * LeakWatch to check that the object actually goes away afterward.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface ExpectUnreachableAfterCleanup {
    /**
     * How long to wait after cleanup before checking reachability.
     *
     * @return grace period in milliseconds
     */
    long gracePeriodMillis() default 0L;

    /**
     * Whether to capture a stack trace when cleanup happens.
     *
     * @return {@code true} to include cleanup-time stack information in reports
     */
    boolean captureCleanupStackTrace() default false;

    /**
     * Extra tags to copy onto retained-after-cleanup reports.
     *
     * @return optional descriptive tags
     */
    String[] tags() default {};
}
