package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * Indicates that the method expects the provided static field references
 * to be NonNull at the execution of the annotated method.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NonNullVariable {
    String[] value();
}
