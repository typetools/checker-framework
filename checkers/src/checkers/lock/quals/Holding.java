package checkers.lock.quals;

import java.lang.annotation.*;

/**
 * Indicates that when the method is invoked, the given locks must be held
 * by the caller.
 *
 * The possible values are explained in {@link GuardedBy} possible values.
 *
 * @see GuardedBy
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Holding {
    String[] value();
}
