package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * Indicates that if the method is a pure method, so calling it
 * multiple times with the same arguments yields the same results.
 * <p>
 *
 * The method should not have any visible side-effect.
 * Non-visible benevolent side effects (e.g., caching) are possible.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {
}
