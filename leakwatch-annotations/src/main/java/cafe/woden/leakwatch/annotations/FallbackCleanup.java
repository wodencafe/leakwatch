package cafe.woden.leakwatch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Supplies a fallback cleanup action for a tracked type.
 *
 * <p>This is for cases where LeakWatch cannot safely invoke the real cleanup method directly,
 * but can still clean up detached state captured from the object.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface FallbackCleanup {
    /**
     * Cleanup action LeakWatch should call when fallback cleanup is needed.
     *
     * @return action class that performs cleanup using detached state
     */
    Class<? extends FallbackCleanupAction<?>> action();
}
