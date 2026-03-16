package cafe.woden.leakwatch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as the explicit cleanup boundary for a {@link LeakTracked} type.
 *
 * <p>Put this on the method that means "this object is done now". Typical examples are
 * {@code close()}, {@code dispose()}, {@code shutdown()}, or {@code disconnect()}.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface CleanupMethod {
}
