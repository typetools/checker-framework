package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * Indicates a method postcondition:  the method expects the specified
 * variables (typically field references) to be non-null when the annotated
 * method is invoked.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface NonNullVariable {
    String[] value();
}
