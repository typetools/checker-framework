package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * Indicates that if the method is a pure method, so calling it
 * with the same parameters would result into the same results.
 * 
 * The method should not have any visible side-effect, and
 * non-visible side-effects (e.g. caching) are possible.
 * 
 * TODO: Consider moving Pure to core quals instead of nullness
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {
}
